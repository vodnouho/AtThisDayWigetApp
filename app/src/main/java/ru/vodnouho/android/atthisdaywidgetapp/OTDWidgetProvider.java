package ru.vodnouho.android.atthisdaywidgetapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
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
    private static final String CONTENT_PROVIDER_PACKAGE = "ru.vodnouho.android.yourday";


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
        Log.d(TAG, "updateAppWidget ID:" + appWidgetId);

        boolean isContentProviderInstalled  =  isPackageInstalled(CONTENT_PROVIDER_PACKAGE,context);
        Log.d(TAG, "is On This Day installed:"+isContentProviderInstalled);
        Log.d(TAG, "is ContentProviderInstalled:"+DataFetcher.isProviderInstalled(context));


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

        RemoteViews rv;
        if(!isContentProviderInstalled){
            rv = new RemoteViews(context.getPackageName(),
                    R.layout.plz_install_widget_layout);

            // Create an Intent to launch Play Market
            final String appPackageName = "ru.vodnouho.android.yourday";
            Intent intent;
            //TODO check is market:// parsed
            try {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
            } catch (android.content.ActivityNotFoundException anfe) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            rv.setOnClickPendingIntent(R.id.installView, pendingIntent);


        }else{
            rv = new RemoteViews(context.getPackageName(),
                    R.layout.atd_widget_layout);

            setTitle(rv, context, settingLang, currentDate);

            //start service for getData and create Views
            setList(rv, context, settingLang, currentDate, appWidgetId);
        }



        //
        // Do additional processing specific to this app widget...
        //
        appWidgetManager.updateAppWidget(appWidgetId, rv);


    }

    private static boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return DataFetcher.isProviderInstalled(context);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
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
            Log.d(TAG, "currentLang:"+currentLang+" settingLang:"+settingLang);

            boolean isLangChaged = false;
            if (!settingLang.equals(currentLang)){
                LocalizationUtils.setLocate(context, settingLang);
                isLangChaged = true;
            }
            titleText = createTitleText(context, currentDate);
            if(isLangChaged){
                LocalizationUtils.setLocate(context, currentLang);
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


        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        String dateS = sdf.format(date);


        // Set up the intent that starts the CategoryListRemoteViewsFactory service, which will
        // provide the views for this collection.
        Intent adapter = new Intent(context, CategoryListRemoteViewsFactory.class);
        adapter.putExtra(UpdateService.EXTRA_WIDGET_ID, appWidgetId);
        adapter.putExtra(UpdateService.EXTRA_WIDGET_LANG, lang);
        adapter.putExtra(UpdateService.EXTRA_WIDGET_DATE, dateS);
        Log.d(TAG, "setRemoteAdapter");
        rv.setRemoteAdapter(R.id.listView, adapter);



    }


    /**
     * Build localized (based on context!!!!) the page title for today, such as "March 21" or "21 Июля"
     * @param context
     * @return
     */
    private static String createTitleText(Context context, Date date) {
        return LocalizationUtils.createLocalizedTitle(context, date);

/*
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
*/
    }


/*
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
*/






}
