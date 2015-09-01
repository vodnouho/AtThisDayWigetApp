package ru.vodnouho.android.atthisdaywidgetapp;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * Created by petukhov on 18.08.2015.
 */
public class UpdateService extends IntentService {
    private static final String TAG = "UpdateService";
    public static final String EXTRA_WIDGET_ID = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_ID";
    public static final String EXTRA_WIDGET_LANG = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_LANG";
    public static final String EXTRA_WIDGET_DATE = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_DATE";
    public static final String EXTRA_WIDGET_TITLE = "ru.vodnouho.android.atthisdaywigetapp.EXTRA_WIDGET_TITLE";


    public UpdateService() {
        super("UpdateService");
    }


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public UpdateService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Widget onHandleIntent Update Service started");
        if(intent != null){
            int wigetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1);
            if(wigetId != -1){

                String lang = intent.getStringExtra(EXTRA_WIDGET_LANG);
                String dateS = intent.getStringExtra(EXTRA_WIDGET_DATE);
                String title = intent.getStringExtra(EXTRA_WIDGET_TITLE);

                //get content
                ArrayList<Category> categories =  getCategories(this, lang, dateS);
                fillCategoriesWithFavoriteFacts(this, categories);


                // Tell the widget manager
                RemoteViews views = ATDWidgetProvider.getViewsRegular(this, lang, title, categories);
                AppWidgetManager manager = AppWidgetManager.getInstance(this);
                manager.updateAppWidget(wigetId, views);

            }
        }
    }

    private static void fillCategoriesWithFavoriteFacts(Context context, ArrayList<Category> categories) {
        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[]{FactsContract.Facts._ID, FactsContract.Facts.TEXT};

        for(Category category : categories) {

            //URI:content://ru.vodnouho.android.yourday.cp/favfacts/08
            Uri contentUri = Uri.withAppendedPath(
                    FactsContract.Facts.FAVCONTENT_URI,
                    category.id);
            Log.d(TAG, "Requesting URI:" + contentUri);


            Cursor categoryCursor = resolver.query(contentUri,
                    projection,
                    null,
                    null,
                    null
            );

            if (categoryCursor == null) {
                continue;
            }

            categoryCursor.moveToFirst();
            while (!categoryCursor.isAfterLast()) {
                String id = categoryCursor.getString(categoryCursor.getColumnIndex(FactsContract.Facts._ID));
                String text = categoryCursor.getString(categoryCursor.getColumnIndex(FactsContract.Facts.TEXT));
                category.add(new Fact(id, text));

                categoryCursor.moveToNext();
            }
            categoryCursor.close();

        }

    }

    /**
     *
     * @param context
     * @param lang
     * @param dateString - format "MMdd"
     */
    public static ArrayList<Category> getCategories(Context context, String lang, String dateString) {

        ArrayList<Category> result = new ArrayList<>();

        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[]{FactsContract.Categories._ID, FactsContract.Categories.NAME};

        //URI:content://ru.vodnouho.android.yourday.cp/categories/en/0818
        Uri contentUri = Uri.withAppendedPath(
                        FactsContract.Categories.CONTENT_URI,
                        lang+"/"+dateString);
        Log.d(TAG, "Requesting URI:"+contentUri);


        Cursor categoryCursor = resolver.query(contentUri,
                projection,
                null,
                null,
                null
        );

        if(categoryCursor == null){
            return null;
        }

        categoryCursor.moveToFirst();
        while (!categoryCursor.isAfterLast()) {
            String id = categoryCursor.getString(categoryCursor.getColumnIndex(FactsContract.Categories._ID));
            String name = categoryCursor.getString(categoryCursor.getColumnIndex(FactsContract.Categories.NAME));
            result.add(new Category(id, name));

            Log.d(TAG, "Category name = " + name);
            categoryCursor.moveToNext();
        }
        categoryCursor.close();

        return result;

    }



    private RemoteViews buildWidgetView(int wigetId) {
        return null;
    }

}
