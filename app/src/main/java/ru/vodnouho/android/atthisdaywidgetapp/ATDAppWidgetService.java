package ru.vodnouho.android.atthisdaywidgetapp;


import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by petukhov on 08.09.2015.
 */
public class ATDAppWidgetService extends RemoteViewsService {
    private static String TAG = "vdnh.WidgetService";
    private static final boolean LOGD = true;

    public static final String EXTRA_WIDGET_ID = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_ID";
    public static final String EXTRA_WIDGET_LANG = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_LANG";
    public static final String EXTRA_WIDGET_DATE = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_DATE";

    public static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";

    private static Object sLock = new Object();


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (LOGD)
            Log.d(TAG, "onGetViewFactory()");
        return new CategoryListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    public static class CategoryListRemoteViewsFactory extends BroadcastReceiver
            implements RemoteViewsFactory, Loader.OnLoadCompleteListener<Cursor>,
            NetworkFetcher.OnLoadListener {

        private Context mContext;
        private String mLang;
        private String mDateString;
        private int mAppWidgetId;

        private ArrayList<Category> mCategories;
        private List<RemoteViewsHolder> mViewsHolder;

        private CursorLoader mCategoryLoader;
        private boolean isLoaderCategoriesFilled = false;

        private int[] mWidgetIds;
        AppWidgetManager mWidgetManager;

        private NetworkFetcher mNetworkFetcher;
        private boolean isNeedNotificationWithoutDataChanging = false;
        private boolean isWatchDogStarted = false;
        private Thread mWatchDogThread;

        private boolean wasErrorOnUrlLoad = false;
        private boolean wasErrorOnImageLoad = false;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Handler mHandler = new Handler();



        private Runnable mImageLoaderWatchDogRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted", e);
                }

                if (LOGD)
                    Log.d(TAG, "WatchDog alive.");
                if (isNeedNotificationWithoutDataChanging) {
                    mWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.listView);
                }

                mWatchDogThread = null;
            }
        };
        private Map<String, ArrayList<Fact>> mImageUrlRequests
                = Collections.synchronizedMap(new HashMap<String, ArrayList<Fact>>());


        public CategoryListRemoteViewsFactory() {
            super();
        }


        public CategoryListRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;

            mLang = intent.getStringExtra(EXTRA_WIDGET_LANG);
            mDateString = intent.getStringExtra(EXTRA_WIDGET_DATE);
            mAppWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            mWidgetManager = AppWidgetManager.getInstance(mContext);
            mWidgetIds = mWidgetManager.getAppWidgetIds(OTDWidgetProvider
                    .getComponentName(mContext));

            mNetworkFetcher = NetworkFetcher.getInstance(mContext);
            if (LOGD)
                Log.d(TAG, "constructor lang:" + mLang + " date:" + mDateString + " appWidgetId:" + mAppWidgetId);

        }


        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOGD)
                Log.d(TAG, "Service got the intent: " + intent.toString());
            mContext = context;

            if (intent.getAction().equals(ACTION_CONNECTIVITY_CHANGE)) {
                if (isOnline(context)) {
                    if (LOGD)
                        Log.d(TAG, "We have an internet!");

                    mWidgetManager =  AppWidgetManager.getInstance(mContext);
                    int[] ids = mWidgetManager.getAppWidgetIds(OTDWidgetProvider
                            .getComponentName(mContext));
                    mWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.listView);
                }
            }
        }


        public boolean isOnline(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            //should check null because in airplane mode it will be null
            return (netInfo != null && netInfo.isConnected());
        }

        @Override
        public void onCreate() {
            if (LOGD)
                Log.d(TAG, "onCreate()");
            mViewsHolder = Collections.synchronizedList(new ArrayList<RemoteViewsHolder>());
            initCategoryLoader(DataFetcher.TYPE_CATEGORIES, null);

        }

        @Override
        public void onDestroy() {
            if (LOGD)
                Log.d(TAG, "onDestroy()");

            if (mCategoryLoader != null) {
                mCategoryLoader.reset();
            }

            stopWatchDogThread();
        }

        @Override
        public void onDataSetChanged() {
            if (LOGD)
                Log.d(TAG, "onDataSetChanged()");

            //no DataProvider - no Data. kekeke!
            if (!DataFetcher.isProviderInstalled(mContext)) {
                return;
            }

            //may be it was image loaded update?
            synchronized (sLock) {
                if (isNeedNotificationWithoutDataChanging) {
                    isNeedNotificationWithoutDataChanging = false;

                    if (LOGD)
                        Log.d(TAG, "onDataSetChanged() return cause isNeedNotificationWithoutDataChanging");

                    return;
                }
            }

            if (mCategories == null) {
                //just created, loader not finished yet
                if (LOGD)
                    Log.d(TAG, "onDataSetChanged() return cause mCategories == null");

                return;
            }

            if(wasErrorOnUrlLoad || wasErrorOnImageLoad){
                if (wasErrorOnUrlLoad) {
                    if (LOGD)
                        Log.d(TAG, "fixing wasErrorOnUrlLoad:" + wasErrorOnUrlLoad);

                    wasErrorOnUrlLoad = false;

                    for (String url : mImageUrlRequests.keySet()) {
                        mNetworkFetcher.requestJsonObject(url, CategoryListRemoteViewsFactory.this);
                    }
                }

                if (wasErrorOnImageLoad) {
                    if (LOGD)
                        Log.d(TAG, "fixing wasErrorOnImageLoad:" + wasErrorOnImageLoad);
                    wasErrorOnImageLoad = false;


                    for (RemoteViewsHolder h : mViewsHolder) {
                        if (h.mFact != null && h.mFact.getThumbnailUrl() != null && h.mImageBitmap == null) {
                            mNetworkFetcher.requestImage(h.mFact.getThumbnailUrl(), CategoryListRemoteViewsFactory.this);
                        }
                    }
                }
                if (LOGD)
                    Log.d(TAG, "onDataSetChanged() return cause wasErrorOnUrlLoad || wasErrorOnImageLoad");
                return;
            }

            //once get localized MORE string
            String localizedMoreString = LocalizationUtils.getLocalizedString(R.string.more, mLang, mContext);


            if (mViewsHolder == null) {
                mViewsHolder = Collections.synchronizedList(new ArrayList<RemoteViewsHolder>());
            } else {
                mViewsHolder.clear();
            }

            //clear favFacts
            for (Category c : mCategories) {
                c.clearFavFacts();
            }

            //TODO lang must be set by ContentProvider
            DataFetcher.fillCategoriesWithFavoriteFacts(mContext, mCategories, mLang);

            for (Category c : mCategories) {
                //category name
                RemoteViews rView = new RemoteViews(mContext.getPackageName(),
                        R.layout.widget_item_category);
                rView.setTextViewText(R.id.tvItemText, c.name);

                setOnClickFillInIntent(rView, R.id.tvItemText, c.id, null);

                mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CATEGORY_NAME));

                ArrayList<Fact> facts = c.getFavFacts();
                for (Fact f : facts) {


                    rView = new RemoteViews(mContext.getPackageName(),
                            R.layout.widget_item);
                    rView.setTextViewText(R.id.tvItemText, Html.fromHtml(f.text));
                    setOnClickFillInIntent(rView, R.id.tvItemText, c.id, f.id);

                    RemoteViewsHolder factViewHolder = new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_FACT);
                    factViewHolder.mFact = f;
                    mViewsHolder.add(factViewHolder);

                    //better start parallel request after mViewsHolder.add()
                    if (factViewHolder.mFact != null && factViewHolder.mFact.getThumbnailUrl() != null) {
                        mNetworkFetcher.requestImage(factViewHolder.mFact.getThumbnailUrl(), this);
                    } else if (f.mayHasThumbnail()) {
                        String findPictureUrlAt = f.getTitleForPicture();
                        if (findPictureUrlAt != null) {
                            addImageUrlRequest(findPictureUrlAt, f);
                            mNetworkFetcher.requestJsonObject(findPictureUrlAt, this);
                        }
                    }
                }

                //add MORE section

/*
                rView = new RemoteViews(mContext.getPackageName(),
                        R.layout.widget_item);

                rView.setTextViewText(R.id.tvItemText, localizedMoreString);
                setOnClickFillInIntent(rView, R.id.tvItemText, c.id, "-1");
                mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_FACT));
*/


            }
        }

        /**
         * Save request
         *
         * @param titleUrl
         * @param f
         */
        private void addImageUrlRequest(String titleUrl, Fact f) {
            ArrayList<Fact> prevRequests = mImageUrlRequests.get(titleUrl);
            if (prevRequests == null) {
                prevRequests = new ArrayList<>();
            }
            prevRequests.add(f);
            mImageUrlRequests.put(titleUrl, prevRequests);
        }

        private boolean isEmptyCategories(ArrayList<Category> categories) {
            for (int i = 0; i < categories.size(); i++) {
                ArrayList<Fact> favFacts = categories.get(i).getFavFacts();
                if (favFacts != null && favFacts.size() > 0) {
                    return false;
                }
            }
            return true;
        }

        private String getImageUrl(Fact f) {
            return f.getThumbnailUrl();
/*
            if (Integer.parseInt(f.id) % 2 == 0) {
                return "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Bryan_Robson_Thailand_2009-11-01_%282%29.jpg/72px-Bryan_Robson_Thailand_2009-11-01_%282%29.jpg";
            }
            return "http://i.imgur.com/7spzG.png";
*/
        }

        /**
         * Query Data
         *
         * @param selection The selection string for the loader to filter the query with.
         */
        public void initCategoryLoader(int type, String selection) {
            if (LOGD)
                Log.d(TAG, "Querying for widget events...");
            // Search for events from now until some time in the future
            Uri uri = null;
            String[] projection = null;

            uri = DataFetcher.createUriForCategories(mLang, mDateString);
            projection = DataFetcher.createProjectionsForCategories();

            mCategoryLoader = new CursorLoader(mContext, uri, projection, selection, null, null);
            mCategoryLoader.registerListener(type, this);
            mCategoryLoader.startLoading();


            if (LOGD)
                Log.d(TAG, "Start loading " + uri);

        }

        private void setOnClickFillInIntent(RemoteViews rv, int viewId, String categoryId, String factId) {
            Bundle extras = new Bundle();
            extras.putString(FactsContract.APP_DATE, mDateString);
            extras.putString(FactsContract.APP_LANG, mLang);
            extras.putString(FactsContract.APP_CATEGORY_ID, categoryId);
            if (factId != null) {
                extras.putString(FactsContract.APP_FACT_ID, factId);
            }
            Intent fillInIntent = new Intent();
            fillInIntent.replaceExtras(extras);
            // Make it possible to distinguish the individual on-click
            // action of a given item
            rv.setOnClickFillInIntent(viewId, fillInIntent);
        }


        @Override
        public int getCount() {
            if (LOGD)
                Log.d(TAG, "getCount()");
            if (mViewsHolder != null) {
                if (LOGD)
                    Log.d(TAG, "getCount() size:" + mViewsHolder.size());
                return mViewsHolder.size();
            }
            return 0;
        }

        //you can fetch some here !!!
        @Override
        public RemoteViews getViewAt(int position) {
            if (mViewsHolder == null || mViewsHolder.size() == 0 || mViewsHolder.size() < (position - 1)) {
                return null;
            }

            RemoteViewsHolder holder = mViewsHolder.get(position);
/*
            if (LOGD) {
                Log.d(TAG, "getViewAt(" + position + ")" );
            }
*/

            // show/hide image
            if (holder.mFact == null || holder.mFact.getThumbnailUrl() == null || holder.mImageBitmap == null) {
                holder.mViews.setViewVisibility(R.id.fact_ImageView, View.GONE);
            } else {
                holder.mViews.setImageViewBitmap(R.id.fact_ImageView, holder.mImageBitmap);
                holder.mViews.setViewVisibility(R.id.fact_ImageView, View.VISIBLE);
            }

            // show/hide year textView
            if (holder.mFact == null || holder.mFact.getYearsAgoString() == null ) {
                holder.mViews.setViewVisibility(R.id.yearItemText, View.GONE);
            }else {
                String localizedYearsAgoString = LocalizationUtils.getLocalizedString(
                        R.string.years_ago, mLang, mContext, holder.mFact.getYearsAgoString());
                holder.mViews.setTextViewText(R.id.yearItemText, localizedYearsAgoString);
                holder.mViews.setViewVisibility(R.id.yearItemText, View.VISIBLE);
            }

            return mViewsHolder.get(position).mViews;
        }

        /*
        If getViewAt() call takes a long time, the loading view
        will be displayed in the corresponding position of the collection view until it returns.
        */
        @Override
        public RemoteViews getLoadingView() {
            if (LOGD)
                Log.d(TAG, "getLoadingView()");
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
            if (LOGD)
                Log.d(TAG, "onLoadComplete() id=" + loader.getId());
            int loaderType = loader.getId();

            if (DataFetcher.TYPE_CATEGORIES == loaderType) {
                mCategories = DataFetcher.fillCategories(data);

                if (mAppWidgetId == -1) {
                    int[] ids = mWidgetManager.getAppWidgetIds(OTDWidgetProvider
                            .getComponentName(mContext));
                    mWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.listView);
                } else {
                    mWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.listView);
                }
            }
        }

        @Override
        public void onImageLoaded(String url, Bitmap bitmap) {
            if (LOGD)
                Log.d(TAG, "onImageLoaded() bitmap.size:" + bitmap.getByteCount());

            if (mViewsHolder == null || mViewsHolder.size() == 0) {
                return;
            }

            for (int i = 0; i < mViewsHolder.size(); i++) {
                RemoteViewsHolder holder = mViewsHolder.get(i);
                Fact fact = holder.mFact;

                if (fact == null) {
                    continue;
                }

                if (holder.mImageBitmap == null
                        && fact.getThumbnailUrl() != null
                        && fact.getThumbnailUrl().equals(url)) {

                    holder.mImageBitmap = bitmap;

                    notifyWithoutDataChanging();
                }
            }

        }

        @Override
        public void onJsonObjectLoaded(String url, JSONObject jsonResponse) {
            if (LOGD)
                Log.d(TAG, "onJsonObjectLoaded():" + jsonResponse.toString());


            if (jsonResponse == null) {
                return;
            }

            ArrayList<Fact> facts = mImageUrlRequests.get(url);
            if (facts == null || facts.size() == 0) {
                //nobody need this info
                return;
            }
            mImageUrlRequests.remove(url);

            JSONObject thumbnail;
            try {
                if (jsonResponse.isNull("thumbnail")) {
                    for (Fact f : facts) {
                        f.setThumbnailUrl("", url);
                        String findPictureUrlAt = f.getTitleForPicture();
                        if (findPictureUrlAt != null) {
                            addImageUrlRequest(findPictureUrlAt, f);
                            mNetworkFetcher.requestJsonObject(findPictureUrlAt, this);
                        }
                    }

                } else {
                    thumbnail = jsonResponse.getJSONObject("thumbnail");
                    String thumbnailUrl = thumbnail.getString("source");

                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        for (Fact f : facts) {
                            f.setThumbnailUrl(thumbnailUrl, url);
                        }
                        mNetworkFetcher.requestImage(thumbnailUrl, this);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "onJsonObjectLoaded:", e);
            }
        }

        @Override
        public void onError(String url, Object error) {
            if (LOGD)
                Log.d(TAG, "onError() reason:" + error.toString());

            ArrayList<Fact> isUrlRequest = mImageUrlRequests.get(url);
            if (isUrlRequest != null) {
                wasErrorOnUrlLoad = true;

                if (LOGD)
                    Log.d(TAG, "wasErrorOnUrlLoad:" + wasErrorOnUrlLoad);

            } else {
                //if not URL error, so it error on image loading
                wasErrorOnImageLoad = true;

                if (LOGD)
                    Log.d(TAG, "wasErrorOnImageLoad:" + wasErrorOnImageLoad);
            }
        }

        private void notifyWithoutDataChanging() {
            synchronized (sLock) {
                isNeedNotificationWithoutDataChanging = true;
            }

            if (mWatchDogThread == null) {
                startWatchDogThread();
            }
        }

        private void startWatchDogThread() {
            mWatchDogThread = new Thread(mImageLoaderWatchDogRunnable);
            isWatchDogStarted = true;
            mWatchDogThread.start();
        }

        private void stopWatchDogThread() {
            isWatchDogStarted = false;
        }


        private class RemoteViewsHolder {
            static final int TYPE_CATEGORY_NAME = 1;
            static final int TYPE_FACT = 2;

            RemoteViews mViews;
            int mType;
            Fact mFact;
            Bitmap mImageBitmap;

            RemoteViewsHolder(RemoteViews rv, int type) {
                mViews = rv;
                mType = type;
            }

        }
    }
}
