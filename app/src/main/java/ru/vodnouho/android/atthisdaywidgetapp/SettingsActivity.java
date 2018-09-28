package ru.vodnouho.android.atthisdaywidgetapp;

/**
 * Activity to set UI settings for instance of widget.
 */

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static ru.vodnouho.android.atthisdaywidgetapp.BuildConfig.DEBUG;
import static ru.vodnouho.android.atthisdaywidgetapp.OTDWidgetProvider.HEADER_TRANSPARENCY_DIFF;

public class SettingsActivity extends AppCompatActivity implements OnThisDayLogic.ModelChangedListener {
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 111;
    private static final String TAG = "vdnh.SettingsActivity";
    private static final String LANG_RU = "ru";
    private static final String LANG_EN = "en";


    private static final String PREFS_NAME
            = "ru.vodnouho.android.atthisdaywigetapp.ATDWidgetProvider";
    private static final String PREF_LANG_KEY = "lang_widget_";
    private static final String PREF_THEME_KEY = "theme_widget_";
    private static final String PREF_TRANSPARENCY_KEY = "transparency_widget_";
    private static final String PREF_TEXT_SIZE_KEY = "textsize_widget_";

    public static final int BASE_TEXT_SIZE_SP = 10;
    public static final int TEXT_SIZE_DIFF = 4;

    public static final String THEME_LIGHT = "1";
    public static final String THEME_BLACK = "2";



    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private String mLang;
    private String mTheme;
    int mBaseBgColor = -1;
    int mBgColor = -1;
    int mTextColor = -1;
    private Date mDate;
    private OnThisDayLogic mLogic;
    private OnThisDayModel mModel;
    private String mDateString;
    private int mTransparency;
    private int mTextSize;

    private ListView mListView;
    private DataAdapter mListAdapter;
    private View mEmptyView;
    private TextView mLoadingView;
    private SeekBar mTransparencySeekBar;
    private SeekBar mTextSizeSeekBar;
    private ImageView mPermissionAlertView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_settings);


        // Find the widget id from the intent.
        Intent intent = getIntent();
        processIntent(intent);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null){
            mLang = savedInstanceState.getString(PREF_LANG_KEY, mLang);
            mTheme = savedInstanceState.getString(PREF_THEME_KEY, mTheme);
            mTransparency = savedInstanceState.getInt(PREF_TRANSPARENCY_KEY, mTransparency);
            mTextSize = savedInstanceState.getInt(PREF_TEXT_SIZE_KEY, mTextSize);
        }

        prepareLangSpinner(this, mLang);
        prepareThemeSpinner(this, mTheme);
        prepareTransparencySeekBar(this, mTransparency);
        drawListView();

        Log.d(TAG, "onRestoreInstanceState mLang:"+mLang);
        Log.d(TAG, "onRestoreInstanceState mTransparency:"+mTransparency);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(outState != null){
            outState.putString(PREF_LANG_KEY, mLang);
            outState.putString(PREF_THEME_KEY, mTheme);
            outState.putInt(PREF_TRANSPARENCY_KEY, mTransparency);
            outState.putInt(PREF_TEXT_SIZE_KEY, mTextSize);
        }
        super.onSaveInstanceState(outState);
    }

    private void processIntent(Intent intent){
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
        Log.d(TAG, "onCreate mLang:"+mLang);

        //Set theme and activate listener for theme spinner
        mTheme = calcTheme(this, mAppWidgetId);
        prepareThemeSpinner(this, mTheme);

        calcBaseColors(mTheme);

        //Set transparency and activate listener for seek bar
        mTransparency = calcTransparency(this, mAppWidgetId);
        prepareTransparencySeekBar(this, mTransparency);
        Log.d(TAG, "onCreate mTransparency:"+mTransparency);

        //Set transparency and activate listener for seek bar
        mTextSize = calcTextSize(this, mAppWidgetId);
        prepareTextSizeSeekBar(this, mTextSize);
        Log.d(TAG, "onCreate mTextSize:"+mTextSize);

        calcBgColor(mBgColor, mTransparency);

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
        ImageView wallpaperImageView = findViewById(R.id.wallpaper_ImageView);
        if(!checkPermissionForReadExtertalStorage(context)){
            wallpaperImageView.setBackgroundColor(getResources().getColor(R.color.linkTextColor));
            return;
        }

        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();


        wallpaperImageView.setImageDrawable(wallpaperDrawable);
    }

    private boolean checkPermissionForReadExtertalStorage(final Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            int result = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if(result == PackageManager.PERMISSION_GRANTED){
                return true;
            }else{
                mPermissionAlertView = findViewById(R.id.needpermission_ImageView);
                mPermissionAlertView.setClickable(true);
                mPermissionAlertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (DEBUG)Log.d(TAG, "checkPermissionForReadExtertalStorage");

                        requestPermissionForReadExtertalStorage( context);

                    }
                });
                mPermissionAlertView.setVisibility(View.VISIBLE);
                return false;
            }
        }
        return true;
    }


    public void requestPermissionForReadExtertalStorage(Context context){
        try {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Can't requestPermissionForReadExtertalStorage",e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissionAlertView.setVisibility(View.GONE);
                    drawWallpaper(this);
                }else{
                    Toast toast = Toast.makeText(this, R.string.request_permission, Toast.LENGTH_LONG);
                    toast.show();
                }
                break;
        }
    }

    private void calcBaseColors(String theme){
        if (SettingsActivity.THEME_LIGHT.equals(theme)) { //mTheme to args
            mBaseBgColor = ContextCompat.getColor(this, R.color.bgColor);
            mTextColor = ContextCompat.getColor(this, R.color.textColor);
        } else {
            mBaseBgColor = ContextCompat.getColor(this, R.color.bgBlackColor);
            mTextColor = ContextCompat.getColor(this, R.color.textBlackColor);
        }
    }

    private void  calcBgColor(int transparency, int bgColor){
        mBgColor = Utils.setTransparency(transparency, bgColor);
    }

    private void drawWidget(String lang, Date date, String theme) {

        ((TextView) findViewById(R.id.loading_textView)).setTextColor(mTextColor);
        findViewById(R.id.emptyView).setBackgroundColor(mBgColor);
        findViewById(R.id.widget_container_ViewGroup).setBackgroundColor(mBgColor);

        //header
        int headerTransparency = Utils.getTransparency(mBgColor) + HEADER_TRANSPARENCY_DIFF;
        if(headerTransparency > 255){
            headerTransparency = 255;
        }
        int headerColor = Utils.setTransparency(headerTransparency, mBgColor);
        findViewById(R.id.title_ViewGroup).setBackgroundColor(headerColor);

        String titleText = LocalizationUtils.createLocalizedTitle(this, lang, date);
        TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
        titleTextView.setTextColor(mTextColor);
        titleTextView.setText(titleText);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,mTextSize + TEXT_SIZE_DIFF);
        ((ImageView) findViewById(R.id.settingsImageButton)).setColorFilter(mTextColor);
    }

    private void  drawListView(){
        mListView = (ListView) findViewById(R.id.listView);
        mListAdapter = new DataAdapter(this);

        if(mModel != null && mModel.categories != null){
            mListAdapter.setData(mModel.categories, mLang);
        }
        mListAdapter.setTheme(this, mTheme);
        mListAdapter.setTransparency(mTransparency);
        mListAdapter.setTextSize(mTextSize);
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


    private int calcTransparency(Context context, int appWidgetId) {
        //transparency
        int transparency = 128;//Utils.getTransparency(mBaseBgColor);

        //search in preference
/*
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        transparency = prefs.getInt(PREF_TRANSPARENCY_KEY + appWidgetId, transparency);
*/
        return loadPrefTransparency(context, appWidgetId, transparency);
    }

    private int calcTextSize(Context context, int appWidgetId) {
        return loadPrefTextSize(context, appWidgetId, BASE_TEXT_SIZE_SP + 4);
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

                    if(mListAdapter != null){
                        mListAdapter.clearData();
                        mListAdapter.notifyDataSetChanged();
                    }

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
                calcBaseColors(mTheme);
                calcTransparency(SettingsActivity.this, mAppWidgetId);
                calcBgColor(mTransparency, mBaseBgColor);
                drawWidget(mLang, mDate, mTheme);
                mListAdapter.setTheme(SettingsActivity.this, mTheme);
                mListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    void prepareTransparencySeekBar(Context context, int transparency) {
        mTransparencySeekBar = findViewById(R.id.transparency_seekBar);
        mTransparencySeekBar.setProgress(invert(transparency));
        mTransparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                   mTransparency = invert(progress);
                   calcBgColor(mTransparency, mBaseBgColor);
                   drawWidget(mLang, mDate, mTheme);
                   mListAdapter.setTransparency(mTransparency);
                   mListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private int invert(int transparency) {
        return 255 - transparency;
    }

    /**
     *
     * @param context
     * @param textSize - in sp
     */
    void prepareTextSizeSeekBar(Context context, int textSize) {
        mTextSizeSeekBar = findViewById(R.id.testSize_seekBar);
        int progress = (textSize - BASE_TEXT_SIZE_SP)/2;
        mTextSizeSeekBar.setProgress(progress);
        mTextSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    mTextSize = progress*2 + BASE_TEXT_SIZE_SP;
                    drawWidget(mLang, mDate, mTheme);
                    mListAdapter.setTextSize(mTextSize);
                    mListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }



    // Write settings to the SharedPreferences object for this widget
    void savePrefs(Context context, int appWidgetId) {

        //save
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_LANG_KEY + appWidgetId, mLang);
        prefs.putString(PREF_THEME_KEY + appWidgetId, mTheme);
        prefs.putInt(PREF_TRANSPARENCY_KEY + appWidgetId, mTransparency);
        prefs.putInt(PREF_TEXT_SIZE_KEY + appWidgetId, mTextSize);
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

    /**
     * Read the theme from the SharedPreferences object for this widget.
     * If there is no preference saved, return SettingsActivity.THEME_LIGHT
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    static int loadPrefTransparency(Context context, int appWidgetId, int defValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        return prefs.getInt(PREF_TRANSPARENCY_KEY + appWidgetId, defValue);
    }

    public static int loadPrefTextSize(Context context, int appWidgetId) {
        return loadPrefTextSize(context, appWidgetId, BASE_TEXT_SIZE_SP);
    }

    static int loadPrefTextSize(Context context, int appWidgetId, int defValue) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int anInt = prefs.getInt(PREF_TEXT_SIZE_KEY + appWidgetId, defValue);
        Log.d(TAG, "loadPrefTextSize mTextSize:"+anInt);
        return anInt;
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
