package ru.vodnouho.android.atthisdaywidgetapp;


import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;


/**
 * Created by petukhov on 08.09.2015.
 */
public class CategoryListRemoteViewsFactory extends RemoteViewsService implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private ArrayList<Category> mCategories;
    private ArrayList<RemoteViewsHolder> mViewsHolder;


    CategoryListRemoteViewsFactory(String lang, String dateS){
        Intent intent = new Intent(this, UpdateService.class);
        intent.putExtra(UpdateService.EXTRA_WIDGET_LANG, lang);
        intent.putExtra(UpdateService.EXTRA_WIDGET_DATE, dateS);

        startService(intent);
    }


    @Override
    public void onCreate() {


    }

    @Override
    public void onDataSetChanged() {
        mViewsHolder = new ArrayList<>();
        if(mCategories != null && mCategories.size()!=0){
            for(Category c : mCategories){
                //category name
                RemoteViews rView = new RemoteViews(mContext.getPackageName(),
                        R.layout.item);
                rView.setTextViewText(R.id.tvItemText,c.name);
                mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CATEGORY_NAME));

                ArrayList<Fact> facts =  c.getFavFacts();
                for(Fact f : facts){
                    //category name
                    rView = new RemoteViews(mContext.getPackageName(),
                            R.layout.item);
                    rView.setTextViewText(R.id.tvItemText, f.text);
                    mViewsHolder.add(new RemoteViewsHolder(rView, RemoteViewsHolder.TYPE_CFACT));

                }
            }
        }
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mViewsHolder.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        return mViewsHolder.get(position).mViews;
    }

    @Override
    public RemoteViews getLoadingView() {
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
        String lang = intent.getStringExtra(UpdateService.EXTRA_WIDGET_LANG);
        String dateS = intent.getStringExtra(UpdateService.EXTRA_WIDGET_DATE);
        return new CategoryListRemoteViewsFactory(lang, dateS);
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
