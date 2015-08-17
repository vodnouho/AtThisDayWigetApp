package ru.vodnouho.android.atthisdaywigetapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;
import android.text.format.Time;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by petukhov on 13.08.2015.
 */
public class ATDWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ATDWidgetProvider";


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            if(!isUpdated(appWidgetId)){
                updateData(appWidgetId);
            }
            // Get the layout for the App Widget and attach an on-click listener
            // to the button

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.atd_widget_layout);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }



    /**
     *
     * @param appWidgetId
     *
     * If your App Widget setup process can take several seconds
     * (perhaps while performing web requests) and you require that your process continues,
     * consider starting a Service in the onUpdate() method.
     */
    private void updateData(int appWidgetId) {

    }

    private boolean isUpdated(int appWidgetId) {
        return false;
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        String settingLang = SettingsActivity.loadPrefs(context, appWidgetId) ;
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();
        if(!currentLang.equals(settingLang)){
            setLocate(context, settingLang);
        }


        // Tell the widget manager
        RemoteViews views = getViewsRegular(context);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static RemoteViews getViewsRegular(Context context) {



        // Build the page title for today, such as "March 21"
        String titleText = createTitleText(context);

        Log.d(TAG, "Title:"+titleText);

        // Construct the RemoteViews object.
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.atd_widget_layout);
        views.setTextViewText(R.id.titleTextView, titleText);

        getCategories(new Date(), context);

        return views;
    }

    /**
     * Build localized the page title for today, such as "March 21" or "21 Июля"
     * @param context
     * @return
     */
    private static String createTitleText(Context context) {
        // Find current month and day
        Time today = new Time();
        today.setToNow();


        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String lang = conf.locale.getLanguage();
        String[] monthNames = res.getStringArray(R.array.month_names);

        StringBuilder titleText = new StringBuilder();
        titleText.append(res.getString(R.string.title));
        if ("ru".equals(lang)) {
            titleText.append(" ");
            titleText.append(today.monthDay);
            titleText.append(" ");
            titleText.append(monthNames[today.month]);
        } else {
            titleText.append(" ");
            titleText.append(monthNames[today.month]);
            titleText.append(" ");
            titleText.append(today.monthDay);
        }



        return titleText.toString();
    }


    private static void setLocate(Context context, String settingLang) {
        Locale newLocale;
        if ("ru".equals(settingLang)) {
            newLocale = new Locale("ru", "RU");
        } else {
            newLocale = Locale.US;
        }

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = newLocale;
        res.updateConfiguration(conf, dm);

    }

    public static void getCategories(Date date, Context context) {
        String lang = context.getResources().getConfiguration().locale.getLanguage();
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        String dateInString  = sdf.format(date);

        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[]{FactsContract.Categories.NAME};
        Uri contentUri = Uri.withAppendedPath(Uri.withAppendedPath(
                        FactsContract.Categories.CONTENT_URI,
                        lang),
                dateInString);

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




}
