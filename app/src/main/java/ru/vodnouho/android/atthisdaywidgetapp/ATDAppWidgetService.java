package ru.vodnouho.android.atthisdaywidgetapp;


import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.vodnouho.android.atthisdaywidgetapp.OTDWidgetProvider.LOGD;


/**
 * Created by petukhov on 08.09.2015.
 * RemoteViewsFactory -> AppWidgetProvider
 * The communication from the RemoteViewsFactory to the AppWidgetProvider can be done using Broadcasts
 */
public class ATDAppWidgetService extends RemoteViewsService {
    private static String TAG = "vdnh.WidgetService";
    //храним живые сервисы по EXTRA_APPWIDGET_ID
    private static ConcurrentHashMap<String, CategoryListRemoteViewsFactory> mapByWidgetId = new ConcurrentHashMap<String, CategoryListRemoteViewsFactory>(3);


    public static final String EXTRA_WIDGET_LANG = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_LANG";
    public static final String EXTRA_WIDGET_DATE = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_DATE";

    public static final String ACTION_IMAGE_LOADED = "ru.vodnouho.android.atthisdaywidgetapp.ACTION_IMAGE_LOADED";
    public static final String ACTION_NO_DATA = "ru.vodnouho.android.atthisdaywidgetapp.ACTION_NO_DATA";
    public static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";

    private static Object sLock = new Object();


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (LOGD)
            Log.d(TAG, "onGetViewFactory()");

        CategoryListRemoteViewsFactory categoryListRemoteViewsFactory = null;

        String lang = intent.getStringExtra(EXTRA_WIDGET_LANG);
        String dateString = intent.getStringExtra(EXTRA_WIDGET_DATE);
        String appWidgetId = "" + intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        //сохраним для виджета
        categoryListRemoteViewsFactory = mapByWidgetId.get(appWidgetId);
        if (categoryListRemoteViewsFactory == null
                || !dateString.equals(categoryListRemoteViewsFactory.getDateString())) {
            categoryListRemoteViewsFactory = new CategoryListRemoteViewsFactory(this.getApplicationContext(), intent);
            mapByWidgetId.put(appWidgetId, categoryListRemoteViewsFactory);
        }


        return categoryListRemoteViewsFactory;
    }

    @Override
    public void onDestroy() {
        SaveLoadHelper.deleteOldFiles(getApplicationContext());
        super.onDestroy();
    }

    public static class CategoryListRemoteViewsFactory extends BroadcastReceiver
            implements RemoteViewsFactory,
            NetworkFetcher.OnLoadListener, OnThisDayLogic.ModelChangedListener {

        private int mTextColor;
        private int mLinkTextColor;
        private int mBgColor;
        private Context mContext;
        private String mLang;
        private String mDateString; //format "MMdd"
        private int mAppWidgetId;

        private OnThisDayModel mModel;
        private List<RemoteViewsHolder> mViewsHolder;


        AppWidgetManager mWidgetManager;

        private NetworkFetcher mNetworkFetcher;
        private boolean isWatchDogStarted = false;
        private Thread mWatchDogThread;

        private boolean wasNoData = false;
        private boolean wasErrorOnUrlLoad = false;
        private boolean wasErrorOnImageLoad = false;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Handler mHandler = new Handler();

        private OnThisDayLogic mLogic;

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

                Intent intent = new Intent(ACTION_IMAGE_LOADED);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                mContext.sendBroadcast(intent);
                NetworkFetcher.saveState();

                mWatchDogThread = null;
            }
        };

        private Runnable mFileClearer = new Runnable() {
            @Override
            public void run() {
                if (LOGD)
                    Log.d(TAG, "mFileClearer alive.");


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

            mNetworkFetcher = NetworkFetcher.getInstance(mContext);

            String settingTheme = SettingsActivity.loadPrefTheme(context, mAppWidgetId);
            if (settingTheme == null || settingTheme.isEmpty()) {
                settingTheme = SettingsActivity.THEME_LIGHT;
            }

            mBgColor = -1;
            mTextColor = -1;
            mLinkTextColor = -1;
            if (SettingsActivity.THEME_LIGHT.equals(settingTheme)) {
                mBgColor = ContextCompat.getColor(context, R.color.bgColor);
                mTextColor = ContextCompat.getColor(context, R.color.textColor);
                mLinkTextColor = ContextCompat.getColor(context, R.color.linkTextColor);
            } else {
                mBgColor = ContextCompat.getColor(context, R.color.bgBlackColor);
                mTextColor = ContextCompat.getColor(context, R.color.textBlackColor);
                mLinkTextColor = ContextCompat.getColor(context, R.color.linkTextBlackColor);
            }


            if (LOGD)
                Log.d(TAG, "constructor lang:" + mLang + " date:" + mDateString + " appWidgetId:" + mAppWidgetId);

        }

        public String getDateString() {
            return mDateString;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOGD)
                Log.d(TAG, "Service got the intent: " + intent.toString());
            mContext = context;

            if (intent.getAction().equals(ACTION_CONNECTIVITY_CHANGE)) {
                if (isOnline(context) && isNeedReloading()) {
                    if (LOGD)
                        Log.d(TAG, "We have an internet!");

                    mWidgetManager = AppWidgetManager.getInstance(mContext);
                    int[] ids = mWidgetManager.getAppWidgetIds(OTDWidgetProvider
                            .getComponentName(mContext));

                    mWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.listView);
                }
            }
        }

        private boolean isNeedReloading() {
            //если нет модели
            if (mModel == null) {
                return true;
            }

            //если модель с пустой коллекцией и не загружается
            if ((mModel.categories == null
                    || mModel.categories.size() < 2) && !mModel.isLoading) {

                return true;
            }

            return false;
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

            mLogic = OnThisDayLogic.getInstance(mDateString, mLang, mContext);
            mLogic.registerModelChangedListener(this);

            mLogic.loadData();

            //init NetworkFetcher
            NetworkFetcher.getInstance(mContext);

        }


        //TODO realize method

        /**
         * check is data cached
         *
         * @return
         */
        private void loadFromCache() {

        }


        @Override
        public void onDestroy() {
            if (LOGD)
                Log.d(TAG, "onDestroy()");

            stopWatchDogThread();
            NetworkFetcher.saveState();
        }

        @Override
        public void onDataSetChanged() {
            if (LOGD)
                Log.d(TAG, "onDataSetChanged()");

            //no DataProvider - no Data. kekeke!
            if (!DataFetcher.isProviderInstalled(mContext)) {
                return;
            }


            if (mModel == null || mModel.categories == null || mModel.categories.size() < 2) {

                mLogic.loadData();
                return;
            }

            if (wasErrorOnUrlLoad) {
                if (LOGD)
                    Log.d(TAG, "fixing wasErrorOnUrlLoad:" + wasErrorOnUrlLoad);

                wasErrorOnUrlLoad = false;
                synchronized (sLock) {
                    for (String url : mImageUrlRequests.keySet()) {
                        mNetworkFetcher.requestJsonObject(url, CategoryListRemoteViewsFactory.this);
                    }
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


        }

        private void updateViews() {
            if(LOGD) Log.d(TAG, "updateViews()");

            //once get localized MORE string
            String localizedMoreString = LocalizationUtils.getLocalizedString(R.string.more, mLang, mContext);


            if (mViewsHolder == null) {
                mViewsHolder = Collections.synchronizedList(new ArrayList<ATDAppWidgetService.CategoryListRemoteViewsFactory.RemoteViewsHolder>());
            } else {
                mViewsHolder.clear();
            }

            if (mModel.categories == null) {
                Log.wtf(TAG, "no mCategories on updateViews called");
                return;
            }


            for (Category c : mModel.categories) {
                //category name
                RemoteViews rView = new RemoteViews(mContext.getPackageName(),
                        R.layout.widget_item_category);


                rView.setTextViewText(R.id.tvItemText, c.name);
                rView.setInt(R.id.tvItemText, "setTextColor", mTextColor);

                setOnClickFillInIntent(rView, R.id.list_item_ViewGroup, c.id, null);

                mViewsHolder.add(new ATDAppWidgetService.CategoryListRemoteViewsFactory.RemoteViewsHolder(rView, ATDAppWidgetService.CategoryListRemoteViewsFactory.RemoteViewsHolder.TYPE_CATEGORY_NAME));

                ArrayList<Fact> facts = c.getFavFacts();
                for (int i = 0; i < facts.size(); i++) {
                    Fact f = facts.get(i);

                    rView = new RemoteViews(mContext.getPackageName(),
                            R.layout.widget_item);
                    rView.setInt(R.id.list_item_ViewGroup, "setBackgroundColor", mBgColor);
                    rView.setTextViewText(R.id.tvItemText, Html.fromHtml(f.text));
                    rView.setInt(R.id.tvItemText, "setTextColor", mTextColor);
                    rView.setInt(R.id.tvItemText, "setLinkTextColor", mLinkTextColor);


                    setOnClickFillInIntent(rView, R.id.list_item_ViewGroup, c.id, f.id);

                    ATDAppWidgetService.CategoryListRemoteViewsFactory.RemoteViewsHolder factViewHolder = new ATDAppWidgetService.CategoryListRemoteViewsFactory.RemoteViewsHolder(rView, ATDAppWidgetService.CategoryListRemoteViewsFactory.RemoteViewsHolder.TYPE_FACT);
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
            synchronized (sLock) {
                mImageUrlRequests.put(titleUrl, prevRequests);
            }
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

            /*if (LOGD) {
                Log.d(TAG, "getViewAt(" + position + ")");
            }*/


            // show/hide image
            if (holder.mFact == null || holder.mFact.getThumbnailUrl() == null || holder.mImageBitmap == null) {
                holder.mViews.setViewVisibility(R.id.fact_ImageView, View.GONE);
            } else {
                holder.mViews.setImageViewBitmap(R.id.fact_ImageView, holder.mImageBitmap);
                holder.mViews.setViewVisibility(R.id.fact_ImageView, View.VISIBLE);
                holder.mViews.setInt(R.id.fact_ImageView, "setBackgroundColor", mBgColor);
            }

            // show/hide year textView
            if (holder.mFact == null || holder.mFact.getYearsAgoString() == null) {
                holder.mViews.setViewVisibility(R.id.yearItemText, View.GONE);

            } else {
                String localizedYearsAgoString = LocalizationUtils.getLocalizedString(
                        R.string.years_ago, mLang, mContext, holder.mFact.getYearsAgoString());
                holder.mViews.setTextViewText(R.id.yearItemText, localizedYearsAgoString);
                holder.mViews.setViewVisibility(R.id.yearItemText, View.VISIBLE);
                holder.mViews.setInt(R.id.yearItemText, "setTextColor", mTextColor);
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


        public void notifyWidgedProviderHasNoData() {
            Intent intent = new Intent(ACTION_NO_DATA);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            mContext.sendBroadcast(intent);
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
            synchronized (sLock) {
                mImageUrlRequests.remove(url);
            }

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

            if (mWatchDogThread == null) {
                startWatchDogThread();
            }

        }

        private void startWatchDogThread() {
            mWatchDogThread = new Thread(mImageLoaderWatchDogRunnable);
            mWatchDogThread.start();
        }

        private void stopWatchDogThread() {
            isWatchDogStarted = false;
        }

        public void notifyOnDataChanged() {
            if (mAppWidgetId == -1 || AppWidgetManager.INVALID_APPWIDGET_ID == mAppWidgetId) {
                int[] ids = mWidgetManager.getAppWidgetIds(OTDWidgetProvider
                        .getComponentName(mContext));
                mWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.listView);
            } else {
                mWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.listView);
            }
        }


        //говорят, что данные изменились
        @Override
        public void onModelChanged(OnThisDayModel newModel) {
            if (LOGD)
                Log.d(TAG, "onModelChanged...");
            if (newModel != mModel) {
                mModel = newModel;
                if (mModel.isError) {
                    notifyWidgedProviderHasNoData();
                } else {
                    //заполним вьюхи
                    updateViews();

                    //расскажем виджету, что пора обновиться
                    notifyOnDataChanged();
                }
            }
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
