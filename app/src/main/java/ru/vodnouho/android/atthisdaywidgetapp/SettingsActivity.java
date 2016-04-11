package ru.vodnouho.android.atthisdaywidgetapp;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static final String LANG_RU = "ru";
    private static final String LANG_EN = "en";



    private static final String PREFS_NAME
            = "ru.vodnouho.android.atthisdaywigetapp.ATDWidgetProvider";
    private static final String PREF_LANG_KEY = "lang_widget_";


    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private String mLang;


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

        // Bind the action for the save button.
        findViewById(R.id.saveButton).setOnClickListener(mOnClickListener);

    }

    private String calcLang(Context context, int appWidgetId) {
        //get current locale
        Locale locale = context.getResources().getConfiguration().locale;
        String lang = locale.getLanguage();

        //is this language available?
        lang = LocalizationUtils.restrictLanguage(lang);

        //search in preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        lang =  prefs.getString(PREF_LANG_KEY + appWidgetId, lang);
        return lang;
    }


    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = SettingsActivity.this;

            // When the button is clicked, save settings with widget prefix
//            String currentWidgetPrefix =  mAppWidgetPrefix.getText().toString();
            savePrefs(context, mAppWidgetId);

            // Push widget update to surface with newly set prefix
            OTDWidgetProvider.updateAppWidget(context, AppWidgetManager.getInstance(getApplication()), mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    void prepareLangSpinner(Context context, String lang){
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
                mLang = langCodes[position];
                //TODO Update argument to preserve selected value on rotation
//                getArguments().putSerializable(FactLab.EXTRA_LANG, mLang);
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
        prefs.apply();
    }

    private int calcSelectedLang(String lang, String[] langCodes) {
        if(lang == null) return 0; //just NullPointException protection
        for(int i=0; i<langCodes.length; i++){
            if (lang.equals(langCodes[i])){
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

    // Read the lang from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadPrefs(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        return prefs.getString(PREF_LANG_KEY + appWidgetId, null);
    }

}
