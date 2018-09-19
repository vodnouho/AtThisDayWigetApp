package ru.vodnouho.android.atthisdaywidgetapp;

/**
 * Activity to set UI settings for instance of widget.
 */

import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static ru.vodnouho.android.atthisdaywidgetapp.BuildConfig.DEBUG;

public class SettingsActivity extends AppCompatActivity implements OnThisDayLogic.ModelChangedListener {
    private static final String TAG = "vdnh.SettingsActivity";
    private static final String LANG_RU = "ru";
    private static final String LANG_EN = "en";


    private static final String PREFS_NAME
            = "ru.vodnouho.android.atthisdaywigetapp.ATDWidgetProvider";
    private static final String PREF_LANG_KEY = "lang_widget_";
    private static final String PREF_THEME_KEY = "theme_widget_";
    public static final String THEME_LIGHT = "1";
    public static final String THEME_BLACK = "2";


    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private String mLang;
    private String mTheme;
    private Date mDate;
    private OnThisDayLogic mLogic;
    private OnThisDayModel mModel;
    private String mDateString;

    private ListView mListView;
    private DataAdapter mListAdapter;
    private View mEmptyView;
    private TextView mLoadingView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_settings);


        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        //Set land and activate listener for lang spinner
        mLang = calcLang(this, mAppWidgetId);
        prepareLangSpinner(this, mLang);

        //Set theme and activate listener for theme spinner
        mTheme = calcTheme(this, mAppWidgetId);
        prepareThemeSpinner(this, mTheme);

        drawWallpaper(this);
        mEmptyView = findViewById(R.id.emptyView);
        mLoadingView = findViewById(R.id.loading_textView);
        mDate = new Date();
        drawWidget(mLang, mDate, mTheme);

        // Bind the action for the save button.
        findViewById(R.id.saveButton).setOnClickListener(mOnClickListener);


        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        mDateString = sdf.format(mDate);
        mLogic = OnThisDayLogic.getInstance(mDateString, mLang, this);
        mLogic.registerModelChangedListener(this);

        mLogic.loadData();

        drawListView();

    }

    private void drawWallpaper(Context context) {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();

        ImageView wallpaperImageView = findViewById(R.id.wallpaper_ImageView);
        wallpaperImageView.setImageDrawable(wallpaperDrawable);
    }

    private void drawWidget(String lang, Date date, String theme) {
        int bgColor = -1;
        int textColor = -1;
        if (SettingsActivity.THEME_LIGHT.equals(theme)) { //mTheme to args

            bgColor = ContextCompat.getColor(this, R.color.bgColor);
            textColor = ContextCompat.getColor(this, R.color.textColor);
        } else {
            bgColor = ContextCompat.getColor(this, R.color.bgBlackColor);
            textColor = ContextCompat.getColor(this, R.color.textBlackColor);
        }

        ((TextView) findViewById(R.id.loading_textView)).setTextColor(textColor);
        findViewById(R.id.emptyView).setBackgroundColor(bgColor);
        findViewById(R.id.widget_container_ViewGroup).setBackgroundColor(bgColor);

        //header
        findViewById(R.id.title_ViewGroup).setBackgroundColor(bgColor);

        String titleText = LocalizationUtils.createLocalizedTitle(this, lang, date);
        TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
        titleTextView.setTextColor(textColor);
        titleTextView.setText(titleText);
        ((ImageView) findViewById(R.id.settingsImageButton)).setColorFilter(textColor);
    }

    private void  drawListView(){
        mListView = (ListView) findViewById(R.id.listView);
        mListAdapter = new DataAdapter(this);

        if(mModel != null && mModel.categories != null){
            mListAdapter.setData(mModel.categories, mLang);
        }
        mListView.setAdapter(mListAdapter);
    }

    private String calcLang(Context context, int appWidgetId) {
        //get current locale
        Locale locale = context.getResources().getConfiguration().locale;
        String lang = locale.getLanguage();

        //is this language available?
        lang = LocalizationUtils.restrictLanguage(lang);

        //search in preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        lang = prefs.getString(PREF_LANG_KEY + appWidgetId, lang);
        return lang;
    }

    private String calcTheme(Context context, int appWidgetId) {
        //search in preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String theme = prefs.getString(PREF_THEME_KEY + appWidgetId, THEME_LIGHT);
        return theme;
    }


    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = SettingsActivity.this;

            // When the button is clicked, save settings with widget prefix
//            String currentWidgetPrefix =  mAppWidgetPrefix.getText().toString();
            savePrefs(context, mAppWidgetId);

            // Push widget update to surface with newly set prefix
            OTDWidgetProvider.updateAppWidget(context, AppWidgetManager.getInstance(getApplication()), mAppWidgetId, false);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    void prepareLangSpinner(Context context, String lang) {
        Spinner spinner = (Spinner) findViewById(R.id.settings_langSpinner);

// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.languages_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);


        final String[] langCodes = context.getResources().getStringArray(R.array.languages_code_array);
        String[] langUIs = context.getResources().getStringArray(R.array.languages_array);

        spinner.setSelection(calcSelectedLang(lang, langCodes));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String langCode = langCodes[position];
                if (langCode != mLang) {
                    mLang = langCode;

                    mLogic.unregisterModelChangedListener(SettingsActivity.this);
                    mLogic = OnThisDayLogic.getInstance(mDateString, mLang, SettingsActivity.this);
                    mLogic.registerModelChangedListener(SettingsActivity.this);

                    showLoading();
                    mLogic.loadData();
                    drawWidget(mLang, mDate, mTheme);
                }
                //TODO Update argument to preserve selected value on rotation
//                getArguments().putSerializable(FactLab.EXTRA_LANG, mLang);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    void prepareThemeSpinner(Context context, String theme) {
        Spinner spinner = (Spinner) findViewById(R.id.settings_themeSpinner);

// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.themes_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);


        final String[] themeCodes = context.getResources().getStringArray(R.array.themes_code_array);

        spinner.setSelection(calcSelectedTheme(theme, themeCodes));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mTheme = themeCodes[position];
                drawWidget(mLang, mDate, mTheme);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }


    // Write settings to the SharedPreferences object for this widget
    void savePrefs(Context context, int appWidgetId) {

        //save
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_LANG_KEY + appWidgetId, mLang);
        prefs.putString(PREF_THEME_KEY + appWidgetId, mTheme);
        prefs.apply();
    }

    private int calcSelectedLang(String lang, String[] langCodes) {
        if (lang == null) return 0; //just NullPointException protection
        for (int i = 0; i < langCodes.length; i++) {
            if (lang.equals(langCodes[i])) {
                return i;
            }
        }
        return 0;
    }

    private int calcSelectedTheme(String theme, String[] themeCodes) {
        Log.d(TAG, "theme:" + theme);
        if (theme == null) return 0; //just NullPointException protection
        for (int i = 0; i < themeCodes.length; i++) {
            if (theme.equals(themeCodes[i])) {
                return i;
            }
        }
        return 0;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Read the lang from the SharedPreferences object for this widget.
     * If there is no preference saved, return the current from a resource
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    static String loadPrefLang(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();

        return prefs.getString(PREF_LANG_KEY + appWidgetId, currentLang);
    }

    /**
     * Read the theme from the SharedPreferences object for this widget.
     * If there is no preference saved, return SettingsActivity.THEME_LIGHT
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    static String loadPrefTheme(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        return prefs.getString(PREF_THEME_KEY + appWidgetId, SettingsActivity.THEME_LIGHT);
    }

    @Override
    public void onModelChanged(OnThisDayModel newModel) {
        if (DEBUG)
            Log.d(TAG, "onModelChanged...");


        if (newModel != mModel) {
            mModel = newModel;
            if (mModel.isError) {
                notifyProviderHasNoData();
            } else {
                //заполним вьюхи
                updateViews(mModel);

                //расскажем виджету, что пора обновиться
                notifyOnDataChanged();
            }
        }
        hideLoading();
    }

    private void hideLoading(){
        mEmptyView.setVisibility(View.GONE);
    }

    private void showLoading(){
        String loadingString = LocalizationUtils.getLocalizedString(R.string.loading_inprogress,
                mLang,
                SettingsActivity.this);
        mLoadingView.setText(loadingString);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    /**
     * TODO implement method
     */
    private void notifyOnDataChanged() {
        if (DEBUG)
            Log.d(TAG, "notifyOnDataChanged...");
    }

    /**
     * TODO implement method
     * @param mModel
     */
    private void updateViews(final OnThisDayModel mModel) {
        if (DEBUG)
            Log.d(TAG, "updateViews...");

        if(mListAdapter != null){
            //change data in UI Thread
            mListView.post(new Runnable() {
                @Override
                public void run() {
                    mListAdapter.setData(mModel.categories, mLang);

                }
            });

        }
    }

    /**
     * TODO implement method
     */
    private void notifyProviderHasNoData() {
        if (DEBUG)
            Log.d(TAG, "notifyWidgedProviderHasNoData...");
    }
}
