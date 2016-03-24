package ru.vodnouho.android.atthisdaywidgetapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created on 13.08.2015.
 */
public class OTDWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ATDWidgetProvider";


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            if(isNeedUpdate(appWidgetId, context)){
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }



    private boolean isNeedUpdate(int appWidgetId, Context context) {
        String settingLang = SettingsActivity.loadPrefs(context, appWidgetId) ;
        if(settingLang == null || settingLang.isEmpty()){
            return false;
        }
        return true;
    }



    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "updateAppWidget ID:"+appWidgetId);
        //save current Lang
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();


        //get settings lang
        String settingLang = SettingsActivity.loadPrefs(context, appWidgetId);
        if(settingLang == null || settingLang.isEmpty()){
            settingLang = currentLang;
        }

        Date currentDate = new Date();

        RemoteViews rv = new RemoteViews(context.getPackageName(),
                R.layout.atd_widget_layout);

        setTitle(rv, context, settingLang, currentDate);

        //start service for getData and create Views
        setList(rv, context, settingLang, currentDate, appWidgetId);



        appWidgetManager.updateAppWidget(appWidgetId, rv);


    }

    private static void setTitle(RemoteViews rv, Context context, String settingLang, Date currentDate) {
        //save current Lang
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();


        String titleText;
        //get title by setting lang. Use context lang, so synchronize it
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (context){
            boolean isLangChaged = false;
            if (!settingLang.equals(currentLang)){
                setLocate(context, settingLang);
                isLangChaged = true;
            }
            titleText = createTitleText(context, currentDate);
            if(isLangChaged){
                setLocate(context, currentLang);
            }
        }


        rv.setTextViewText(R.id.titleTextView, titleText);
    }


    /**
     *
     * @param context
     * @param lang
     * @param date
     * @param appWidgetId
     * If your App Widget setup process can take several seconds
     * (perhaps while performing web requests) and you require that your process continues,
     * consider starting a Service in the onUpdate() method.
     */
    private static void setList(RemoteViews rv, Context context, String lang, Date date, int appWidgetId) {
        Log.d(TAG, "Starting service");

        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        String dateS = sdf.format(date);


        Intent adapter = new Intent(context, CategoryListRemoteViewsFactory.class);
        adapter.putExtra(UpdateService.EXTRA_WIDGET_ID, appWidgetId);
        adapter.putExtra(UpdateService.EXTRA_WIDGET_LANG, lang);
        adapter.putExtra(UpdateService.EXTRA_WIDGET_DATE, dateS);
        rv.setRemoteAdapter(R.id.listView, adapter);



    }


    /**
     * Build localized (based on context!!!!) the page title for today, such as "March 21" or "21 Июля"
     * @param context
     * @return
     */
    private static String createTitleText(Context context, Date date) {
        // Find current month and day
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int monthDay = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);

        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String lang = conf.locale.getLanguage();
        String[] monthNames = res.getStringArray(R.array.month_names);

        StringBuilder titleText = new StringBuilder();
        titleText.append(res.getString(R.string.title));
        if ("ru".equals(lang)) {
            titleText.append(" ");
            titleText.append(calendar.get(Calendar.DAY_OF_MONTH));
            titleText.append(" ");
            titleText.append(monthNames[month]);
        } else {
            titleText.append(" ");
            titleText.append(monthNames[month]);
            titleText.append(" ");
            titleText.append(monthDay);
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






}
