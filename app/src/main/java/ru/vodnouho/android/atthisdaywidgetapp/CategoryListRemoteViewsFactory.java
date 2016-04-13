package ru.vodnouho.android.atthisdaywidgetapp;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;


/**
 * Created by petukhov on 08.09.2015.
 */
public class CategoryListRemoteViewsFactory extends RemoteViewsService implements RemoteViewsService.RemoteViewsFactory {
    private static String TAG = "vdnh.CategoryListRemoteViewsFactory";
    public static final String EXTRA_WIDGET_ID = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_ID";
    public static final String EXTRA_WIDGET_LANG = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_LANG";
    public static final String EXTRA_WIDGET_DATE = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_DATE";

    private Context mContext;
    private String mLang;
    private String mDateString;
    private int mAppWidgetId;


    private ArrayList<Category> mCategories;
    private ArrayList<RemoteViewsHolder> mViewsHolder;



    public CategoryListRemoteViewsFactory(){
        super();
    }

    public CategoryListRemoteViewsFactory(Context context, Intent intent){
        mContext = context;

        mLang = intent.getStringExtra(EXTRA_WIDGET_LANG);
        mDateString = intent.getStringExtra(EXTRA_WIDGET_DATE);
        mAppWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        Log.d(TAG, "constructor lang:" + mLang + " date:" + mDateString + " appWidgetId:" + mAppWidgetId);

    }



    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        mViewsHolder = new ArrayList<>();
    }

    @Override
    public void onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged()");

        if(mViewsHolder != null){
            mViewsHolder.clear();
        }

        if(!DataFetcher.isProviderInstalled(mContext)){
            return;
        }

        mCategories = DataFetcher.getCategories(mContext, mLang, mDateString);

        if(mCategories != null && mCategories.size()!=0){
            DataFetcher.fillCategoriesWithFavoriteFacts(mContext, mCategories);

            mViewsHolder = new ArrayList<>();
            for(Category c : mCategories){
                //category name
                RemoteViews rView = new RemoteViews(mContext.getPackageName(),
                        R.layout.widget_item);
                rView.setTextViewText(R.id.tvItemText,c.name);

                setOnClickFillInIntent(rView, R.id.tvItemText, c.id, null);

                mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CATEGORY_NAME));

                ArrayList<Fact> facts =  c.getFavFacts();
                for(Fact f : facts){
                    //category name
                    rView = new RemoteViews(mContext.getPackageName(),
                            R.layout.widget_item);
                    rView.setTextViewText(R.id.tvItemText, f.text);

                    setOnClickFillInIntent(rView, R.id.tvItemText, c. id, f.id);

                    mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CFACT));
                }
            }
        }
    }

    private void setOnClickFillInIntent(RemoteViews rv, int viewId, String categoryId, String factId){
        Bundle extras = new Bundle();
        extras.putString(FactsContract.APP_DATE, mDateString);
        extras.putString(FactsContract.APP_LANG, mLang);
        extras.putString(FactsContract.APP_CATEGORY_ID, categoryId);
        if(factId != null){
            extras.putString(FactsContract.APP_FACT_ID, factId);
        }
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        // Make it possible to distinguish the individual on-click
        // action of a given item
        rv.setOnClickFillInIntent(viewId, fillInIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public int getCount() {
        Log.d(TAG, "getCount()");
        if(mViewsHolder != null){
            Log.d(TAG, "getCount() size:"+mViewsHolder.size());
            return mViewsHolder.size();
        }
        return 0;
    }

    //you can fetch some here !!!
    @Override
    public RemoteViews getViewAt(int position) {
        Log.d(TAG, "getViewAt("+position+")");

        return mViewsHolder.get(position).mViews;
    }

    /*
    If getViewAt() call takes a long time, the loading view
    will be displayed in the corresponding position of the collection view until it returns.
    */
    @Override
    public RemoteViews getLoadingView() {
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
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
/*
        String lang = intent.getStringExtra(UpdateService.EXTRA_WIDGET_LANG);
        String dateS = intent.getStringExtra(UpdateService.EXTRA_WIDGET_DATE);
        return new CategoryListRemoteViewsFactory(lang, dateS);
 */
        Log.d(TAG, "onGetViewFactory()");
        return new CategoryListRemoteViewsFactory(this.getApplicationContext(), intent);
    }


    private class RemoteViewsHolder {
        static final int TYPE_CATEGORY_NAME = 1;
        static final int TYPE_CFACT = 2;

        RemoteViews mViews;
        int mType;

        RemoteViewsHolder(RemoteViews rv, int type){
            mViews = rv;
            mType = type;
        }

    }
}
