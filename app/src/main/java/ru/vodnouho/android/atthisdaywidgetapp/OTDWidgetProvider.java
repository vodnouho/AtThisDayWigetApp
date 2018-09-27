package ru.vodnouho.android.atthisdaywidgetapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.vodnouho.android.atthisdaywidgetapp.BuildConfig.DEBUG;

/**
 * Created on 13.08.2015.
 */
public class OTDWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "vdnh.OTDWidgetProvider";
    public static final String APPWIDGET_CONFIGURE = "ru.vodnouho.android.atthisdaywidgetapp.APPWIDGET_CONFIGURE";
    public static final String APPWIDGET_CONFIGURE_URI ="otd://ru.vodnouho.android.atthisdaywidgetapp.SettingsActivity/";

    public static final boolean LOGD = true;

    private static final String CONTENT_PROVIDER_PACKAGE = "ru.vodnouho.android.yourday";
    public static final String RUN_ACTION = "ru.vodnouho.android.RUN_ACTION"; //Action for run OTD app
    public static final String ACTION_REFRESH = "ru.vodnouho.android.ACTION_REFRESH"; //Action for refresh widget

    public static final String ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED"; //Action for refresh widget

    public static final String EXTRA_ITEM = "ru.vodnouho.android.EXTRA_ITEM"; //Action for run OTD app
    public static final int HEADER_TRANSPARENCY_DIFF = 32;




    // Called when the BroadcastReceiver receives an Intent broadcast.
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOGD)
            Log.d(TAG, "OTDWidgetProvider got the action:"+intent.getAction()+" intent: " + intent.toString());
        super.onReceive(context, intent);

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(RUN_ACTION)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            String extra = intent.getStringExtra(EXTRA_ITEM);
            Toast.makeText(context, "Touched view " + extra, Toast.LENGTH_SHORT).show();

        } else if (intent.getAction().equals(ACTION_DATE_CHANGED)){
            int[] appWidgetIds = mgr.getAppWidgetIds(
                    new ComponentName(context, OTDWidgetProvider.class)
            );
            for(int i=0; i<appWidgetIds.length; i++){
                updateAppWidget(context, mgr, appWidgetIds[i], false);
            }

        }else if (intent.getAction().equals(ACTION_REFRESH) ) {
            Log.d(TAG, "OTDWidgetProvider catch the intent: " + intent.toString());
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        } else if (intent.getAction().equals(ATDAppWidgetService.ACTION_IMAGE_LOADED)) {
            if (LOGD)
                Log.d(TAG, " Image loaded ! widgetId:" + intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        } else if (intent.getAction().equals(ATDAppWidgetService.ACTION_NO_DATA)) {
            if (LOGD)
                Log.d(TAG, "No data received" + intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            updateAppWidget(context, appWidgetManager, appWidgetId, true);
        } else {
            //super.onReceive(context, intent);
        }

    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");

        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            if (isNeedUpdate(appWidgetId, context)) {
                updateAppWidget(context, appWidgetManager, appWidgetId, false);
            }
        }
    }


    /**
     * Build {@link ComponentName} describing this specific
     * {@link AppWidgetProvider}
     */
    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, OTDWidgetProvider.class);
    }

    private boolean isNeedUpdate(int appWidgetId, Context context) {
        String settingLang = SettingsActivity.loadPrefLang(context, appWidgetId);
        if (settingLang == null || settingLang.isEmpty()) {
            return false;
        }
        return true;
    }


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean isNoData) {
        Log.d(TAG, "updateAppWidget ID:" + appWidgetId);

        boolean isContentProviderInstalled = isPackageInstalled(CONTENT_PROVIDER_PACKAGE, context);


        RemoteViews rv;
        if (!isContentProviderInstalled) {
            rv = createViewPleaseInstall(context, appWidgetId);
        } else {
            rv = createView(context, appWidgetId, isNoData);
        }


        //
        // Do additional processing specific to this app widget...
        //
        appWidgetManager.updateAppWidget(appWidgetId, rv);


    }

    private static RemoteViews createView(Context context, int appWidgetId, boolean isNoData){
        //no content provider

        RemoteViews rv = new RemoteViews(context.getPackageName(),
                R.layout.atd_widget_layout);

        //save current Lang
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();


        //get settings lang
        String settingLang = SettingsActivity.loadPrefLang(context, appWidgetId);
        if (settingLang == null || settingLang.isEmpty()) {
            settingLang = currentLang;
        }

        String settingTheme = SettingsActivity.loadPrefTheme(context, appWidgetId);
        if (settingTheme == null || settingTheme.isEmpty()) {
            settingTheme = SettingsActivity.THEME_LIGHT;
        }

        int bgColor = -1;
        int textColor = -1;
        if(SettingsActivity.THEME_LIGHT.equals(settingTheme)){

            bgColor = ContextCompat.getColor(context, R.color.bgColor);
            textColor = ContextCompat.getColor(context, R.color.textColor);
        }else{
            bgColor = ContextCompat.getColor(context, R.color.bgBlackColor);
            textColor = ContextCompat.getColor(context, R.color.textBlackColor);
        }

        int transparency = SettingsActivity.loadPrefTransparency(context, appWidgetId, 128);
        bgColor = Utils.setTransparency(transparency, bgColor);

        int textSize = SettingsActivity.loadPrefTextSize(context, appWidgetId);

        if(DEBUG)  Log.d(TAG, "textSize:"+textSize);

        rv.setInt(R.id.loading_textView, "setTextColor", textColor);
        rv.setInt(R.id.emptyView, "setBackgroundColor", bgColor);
        rv.setInt(R.id.widget_container_ViewGroup, "setBackgroundColor", bgColor);

        //header
        Date currentDate = new Date();
        setTitleText(rv, context, settingLang, currentDate);
        rv.setInt(R.id.titleTextView, "setTextColor", textColor);
        rv.setFloat(R.id.titleTextView, "setTextSize", textSize + SettingsActivity.TEXT_SIZE_DIFF);

        int headerTransparency = transparency + HEADER_TRANSPARENCY_DIFF;
        if(headerTransparency > 255){
            headerTransparency = 255;
        }
        int headerBgColor = Utils.setTransparency(headerTransparency, bgColor);
        rv.setInt(R.id.title_ViewGroup, "setBackgroundColor", headerBgColor);

        rv.setInt(R.id.settingsImageButton, "setColorFilter", textColor);

        rv.setOnClickPendingIntent(R.id.titleTextView, getPendingSelfIntent(context, ACTION_REFRESH, appWidgetId));
        rv.setOnClickPendingIntent(R.id.settingsImageButton, getSettingIntent(context,
                AppWidgetManager.ACTION_APPWIDGET_CONFIGURE,
                appWidgetId));

        //start service for getData and create Views
        setList(rv, context, settingLang, currentDate, appWidgetId, isNoData);

        return rv;
    }

    private static RemoteViews createViewPleaseInstall(Context context, int appWidgetId){
        //no content provider

        RemoteViews rv = new RemoteViews(context.getPackageName(),
                R.layout.plz_install_widget_layout);

        //save current Lang
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();


        //get settings lang
        String settingLang = SettingsActivity.loadPrefLang(context, appWidgetId);
        if (settingLang == null || settingLang.isEmpty()) {
            settingLang = currentLang;
        }

        String settingTheme = SettingsActivity.loadPrefTheme(context, appWidgetId);
        if (settingTheme == null || settingTheme.isEmpty()) {
            settingTheme = SettingsActivity.THEME_LIGHT;
        }

        int bgColor = -1;
        int headerBgColor = -1;
        int textColor = -1;
        if(SettingsActivity.THEME_LIGHT.equals(settingTheme)){

            bgColor = ContextCompat.getColor(context, R.color.bgColor);
            headerBgColor = ContextCompat.getColor(context, R.color.headerBgColor);
            textColor = ContextCompat.getColor(context, R.color.textColor);
        }else{
            bgColor = ContextCompat.getColor(context, R.color.bgBlackColor);
            headerBgColor = ContextCompat.getColor(context, R.color.headerBgBlackColor);
            textColor = ContextCompat.getColor(context, R.color.textBlackColor);
        }


        rv.setInt(R.id.plz_install_ViewGroup, "setBackgroundColor", bgColor);


        String plzInstallString = LocalizationUtils.getLocalizedString(R.string.plz_install_otd, settingLang, context);
        rv.setTextViewText(R.id.plz_install_otd_TextView, plzInstallString);
        rv.setInt(R.id.plz_install_otd_TextView, "setTextColor", textColor);

        plzInstallString = LocalizationUtils.getLocalizedString(R.string.install, settingLang, context);
        rv.setTextViewText(R.id.installButton, plzInstallString);
        //rv.setInt(R.id.installButton, "setTextColor", textColor);


        // Create an Intent to launch Play Market

        Intent intent;

        try {
            //TODO link to BETA !!!
            //intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/apps/testing/ru.vodnouho.android.yourday"));
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + CONTENT_PROVIDER_PACKAGE));
        } catch (android.content.ActivityNotFoundException anfe) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + CONTENT_PROVIDER_PACKAGE));
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        rv.setOnClickPendingIntent(R.id.installButton, pendingIntent);

        plzInstallString = LocalizationUtils.getLocalizedString(R.string.plz_resfresh, settingLang, context);
        rv.setTextViewText(R.id.plz_refresh_otd_TextView, plzInstallString);
        rv.setInt(R.id.plz_refresh_otd_TextView, "setTextColor", textColor);

        plzInstallString = LocalizationUtils.getLocalizedString(R.string.refresh, settingLang, context);
        rv.setTextViewText(R.id.refreshButton, plzInstallString);

        rv.setOnClickPendingIntent(R.id.refreshButton, getPendingSelfIntent(context, ACTION_REFRESH, appWidgetId));

        return rv;

    }

    protected static PendingIntent getSettingIntent(Context context, String action, int widgetId) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.setAction(action);
        intent.setData(Uri.parse(APPWIDGET_CONFIGURE_URI+widgetId));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        if (LOGD)
            Log.d(TAG, "getSettingIntent widgetId=" + widgetId + " intent" + intent);

        return PendingIntent.getActivity(context, widgetId, intent, 0);
    }

    protected static PendingIntent getPendingSelfIntent(Context context, String action, int widgetId) {
        Intent intent = new Intent(context, OTDWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        if (LOGD)
            Log.d(TAG, "getPendingSelfIntent widgetId=" + widgetId + " intent" + intent);

        return PendingIntent.getBroadcast(context, widgetId, intent, 0);
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

    private static void setTitleText(RemoteViews rv, Context context, String settingLang, Date currentDate) {
        //save current Lang
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();




        String titleText;
        //get title by setting lang. Use context lang, so synchronize it
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (context) {
            Log.d(TAG, "setTitleText currentLang:" + currentLang + " settingLang:" + settingLang);

            titleText = LocalizationUtils.createLocalizedTitle(context, settingLang, currentDate);

/*
            boolean isLangChaged = false;
            if (!settingLang.equals(currentLang)) {
                LocalizationUtils.setLocate(context, settingLang);
                isLangChaged = true;
            }
            titleText = createTitleText(context, currentDate);
            if (isLangChaged) {
                LocalizationUtils.setLocate(context, currentLang);
            }
*/
        }

        rv.setTextViewText(R.id.titleTextView, titleText);

    }


    /**
     * @param context
     * @param lang
     * @param date
     * @param appWidgetId If your App Widget setup process can take several seconds
     *                    (perhaps while performing web requests) and you require that your process continues,
     *                    consider starting a Service in the onUpdate() method.
     */
    private static void setList(RemoteViews rv, Context context, String lang, Date date, int appWidgetId, boolean isNoData) {


        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        String dateS = sdf.format(date);


        String settingTheme = SettingsActivity.loadPrefTheme(context, appWidgetId);
        if (settingTheme == null || settingTheme.isEmpty()) {
            settingTheme = SettingsActivity.THEME_LIGHT;
        }

        int bgColor = -1;
        int textColor = -1;
        if(SettingsActivity.THEME_LIGHT.equals(settingTheme)){
            bgColor = ContextCompat.getColor(context,R.color.bgColor);
            textColor = ContextCompat.getColor(context,R.color.textColor);
        }else{
            bgColor = ContextCompat.getColor(context,R.color.bgBlackColor);
            textColor = ContextCompat.getColor(context,R.color.textBlackColor);
        }

        int transparency = SettingsActivity.loadPrefTransparency(context, appWidgetId, 128);
        bgColor = Utils.setTransparency(transparency, bgColor);

        int textSize = SettingsActivity.loadPrefTextSize(context, appWidgetId);

        // Set up the intent that starts the ATDAppWidgetService service, which will
        // provide the views for this collection.
        Intent adapter = new Intent(context, ATDAppWidgetService.class);
//        adapter.putExtra(CategoryListRemoteViewsFactory.EXTRA_WIDGET_ID, appWidgetId);
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);        //TODO???
        adapter.putExtra(ATDAppWidgetService.EXTRA_WIDGET_LANG, lang);
        adapter.putExtra(ATDAppWidgetService.EXTRA_WIDGET_THEME, settingTheme);
        adapter.putExtra(ATDAppWidgetService.EXTRA_WIDGET_DATE, dateS);
        adapter.putExtra(ATDAppWidgetService.EXTRA_BG_COLOR, bgColor);
        adapter.putExtra(ATDAppWidgetService.EXTRA_TEXT_SIZE, textSize );
        adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
        Log.d(TAG, "setRemoteAdapter id:"+appWidgetId + " lang:"+lang + " dateS:"+dateS);
        rv.setRemoteAdapter(R.id.listView, adapter);
        //rv.setInt(R.id.listView, "setBackgroundColor", bgColor);


        rv.setEmptyView(R.id.listView, R.id.emptyView);
        rv.setInt(R.id.emptyView, "setBackgroundColor", bgColor);

        if (isNoData) {
            String localizedNoDataString = LocalizationUtils.getLocalizedString(R.string.loading_error, lang, context);
            rv.setTextViewText(R.id.loading_textView, localizedNoDataString);
            rv.setInt(R.id.loading_textView, "setTextColor", textColor);
        }


        // This section makes it possible for items to have individualized behavior.
        // It does this by setting up a pending intent template. Individuals items of a collection
        // cannot set up their own pending intents. Instead, the collection as a whole sets
        // up a pending intent template, and the individual items set a fillInIntent
        // to create unique behavior on an item-by-item basis.
        Intent appIntent = new Intent();
        appIntent.setClassName(CONTENT_PROVIDER_PACKAGE, "ru.vodnouho.android.yourday.HomeActivity");

        // Set the action for the intent.
        // When the user touches a particular view, it will have the effect of
        // broadcasting TOAST_ACTION.
        appIntent.setAction(Intent.ACTION_VIEW);
        // Use appWidgetId por prevent reuse of Intent on different appwidget instance
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, appIntent, 0);
        rv.setPendingIntentTemplate(R.id.listView, pendingIntent);

        if(!isNoData){
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.listView);
        }


    }


    /**
     * Build localized (based on context!!!!) the page title for today, such as "March 21" or "21 Июля"
     *
     * @param context
     * @return
     */
    private static String createTitleText(Context context, Date date) {
        return LocalizationUtils.createLocalizedTitle(context, date);


    }

    @Override
    public void onDisabled(Context context) {
        NetworkFetcher.saveState();
        super.onDisabled(context);
    }
}
