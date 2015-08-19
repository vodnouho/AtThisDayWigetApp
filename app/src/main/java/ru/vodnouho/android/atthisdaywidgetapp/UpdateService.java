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
                getCategories(this, lang, dateS);

                // Tell the widget manager
                RemoteViews views = ATDWidgetProvider.getViewsRegular(this, lang, title);
                AppWidgetManager manager = AppWidgetManager.getInstance(this);
                manager.updateAppWidget(wigetId, views);

            }
        }
    }

    /**
     *
     * @param context
     * @param lang
     * @param dateString - format "MMdd"
     */
    public static void getCategories(Context context, String lang, String dateString) {

        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[]{FactsContract.Categories.NAME};

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
            return;
        }

        categoryCursor.moveToFirst();
        while (!categoryCursor.isAfterLast()) {
            String name = categoryCursor.getString(categoryCursor.getColumnIndex(FactsContract.Categories.NAME));
            Log.d(TAG, "Category name = " + name);
            categoryCursor.moveToNext();
        }

    }



    private RemoteViews buildWidgetView(int wigetId) {
        return null;
    }

}
