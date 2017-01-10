package ru.vodnouho.android.atthisdaywidgetapp;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;


/**
 * Created by petukhov on 08.09.2015.
 */
public class ATDAppWidgetService extends RemoteViewsService {
    private static String TAG = "vdnh.WidgetService";
    private static final boolean LOGD = true;

    public static final String EXTRA_WIDGET_ID = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_ID";
    public static final String EXTRA_WIDGET_LANG = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_LANG";
    public static final String EXTRA_WIDGET_DATE = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_DATE";


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (LOGD)
            Log.d(TAG, "onGetViewFactory()");
        return new CategoryListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    public static class CategoryListRemoteViewsFactory
            implements RemoteViewsService.RemoteViewsFactory, Loader.OnLoadCompleteListener<Cursor> {
        private Context mContext;
        private String mLang;
        private String mDateString;
        private int mAppWidgetId;

        private ArrayList<Category> mCategories;
        private ArrayList<Category> mCategoriesN;
        private ArrayList<RemoteViewsHolder> mViewsHolder;

        private CursorLoader mCategoryLoader;


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
            if (LOGD)
                Log.d(TAG, "constructor lang:" + mLang + " date:" + mDateString + " appWidgetId:" + mAppWidgetId);

        }


        @Override
        public void onCreate() {
            if (LOGD)
                Log.d(TAG, "onCreate()");
            mViewsHolder = new ArrayList<>();
            //init Categories Loader
            initLoader(DataFetcher.TYPE_CATEGORIES, null);
        }

        @Override
        public void onDataSetChanged() {
            if (LOGD)
                Log.d(TAG, "onDataSetChanged()");

            if (mViewsHolder != null) {
                mViewsHolder.clear();
            }

            if (!DataFetcher.isProviderInstalled(mContext)) {
                return;
            }

            //mCategories = DataFetcher.getCategories(mContext, mLang, mDateString);

            //once get localized MORE string
            String localizedMoreString = LocalizationUtils.getLocalizedString(R.string.more, mLang, mContext);

            if(mCategories == null || mCategories.size() == 0){
                if(mCategoryLoader == null){
                   // initLoader(DataFetcher.TYPE_CATEGORIES, null);
                }
            } else {
                DataFetcher.fillCategoriesWithFavoriteFacts(mContext, mCategories);

                mViewsHolder = new ArrayList<>();
                for (Category c : mCategories) {
                    //category name
                    RemoteViews rView = new RemoteViews(mContext.getPackageName(),
                            R.layout.widget_item_category);
                    rView.setTextViewText(R.id.tvItemText, c.name);

                    setOnClickFillInIntent(rView, R.id.tvItemText, c.id, null);

                    mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CATEGORY_NAME));

                    ArrayList<Fact> facts = c.getFavFacts();
                    for (Fact f : facts) {
                        //category name
                        rView = new RemoteViews(mContext.getPackageName(),
                                R.layout.widget_item);
                        rView.setTextViewText(R.id.tvItemText, Html.fromHtml(f.text));

                        setOnClickFillInIntent(rView, R.id.tvItemText, c.id, f.id);

                        mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CFACT));
                    }

                    //add MORE section

                    rView = new RemoteViews(mContext.getPackageName(),
                            R.layout.widget_item);

                    rView.setTextViewText(R.id.tvItemText, localizedMoreString);
                    setOnClickFillInIntent(rView, R.id.tvItemText, c.id, "-1");
                    mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CFACT));
                }


            }
        }

        /**
         * Query Data
         *
         * @param selection The selection string for the loader to filter the query with.
         */
        public void initLoader(int type, String selection) {
            if (LOGD)
                Log.d(TAG, "Querying for widget events...");
            // Search for events from now until some time in the future
            Uri uri = null;
            String[] projection = null;
            CursorLoader loader = null;

            if(DataFetcher.TYPE_CATEGORIES == type){
                uri = DataFetcher.createUriForCategories(mLang, mDateString);
                projection = DataFetcher.createProjectionsForCategories();
                mCategoryLoader = loader;

            }
            loader = new CursorLoader(mContext, uri, projection, selection, null, null);
            loader.registerListener(type, this);
            loader.startLoading();

            if (LOGD)
                Log.d(TAG, "Start loading "+uri);

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
        public void onDestroy() {
            if (LOGD)
                Log.d(TAG, "onDestroy()");

                if (mCategoryLoader != null) {
                    mCategoryLoader.reset();
                }
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
            if (LOGD)
                Log.d(TAG, "getViewAt(" + position + ")");

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
                Log.d(TAG, "onLoadComplete() id="+loader.getId());
            int loaderType = loader.getId();

            if(DataFetcher.TYPE_CATEGORIES == loaderType){
                mCategories = DataFetcher.fillCategories(data);

                AppWidgetManager widgetManager = AppWidgetManager.getInstance(mContext);
                if (mAppWidgetId == -1) {
                    int[] ids = widgetManager.getAppWidgetIds(OTDWidgetProvider
                            .getComponentName(mContext));
                    widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.listView);
                } else {
                    widgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.listView);
                }
            }
        }


        private class RemoteViewsHolder {
            static final int TYPE_CATEGORY_NAME = 1;
            static final int TYPE_CFACT = 2;

            RemoteViews mViews;
            int mType;

            RemoteViewsHolder(RemoteViews rv, int type) {
                mViews = rv;
                mType = type;
            }

        }
    }
}
