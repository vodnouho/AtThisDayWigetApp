package ru.vodnouho.android.atthisdaywigetapp;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

public class SettingsActivity extends AppCompatActivity {
    private static final String LANG_RU = "ru";
    private static final String LANG_EN = "en";

    private static final String PREFS_NAME
            = "ru.vodnouho.android.atthisdaywigetapp.ATDWidgetProvider";
    private static final String PREF_LANG_KEY = "lang_widget_";


    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_settings);

        // Bind the action for the save button.
        findViewById(R.id.saveButton).setOnClickListener(mOnClickListener);

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

    }


    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = SettingsActivity.this;

            // When the button is clicked, save settings with widget prefix
//            String currentWidgetPrefix =  mAppWidgetPrefix.getText().toString();
            savePrefs(context, mAppWidgetId);

            // Push widget update to surface with newly set prefix
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ATDWidgetProvider.updateAppWidget(context, appWidgetManager,
                    mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    // Write settings to the SharedPreferences object for this widget
    void savePrefs(Context context, int appWidgetId) {

        //find selected land
        String lang;
        RadioGroup langSettingsView = (RadioGroup) findViewById(R.id.langSetting);
        int checkedId = langSettingsView.getCheckedRadioButtonId();
        switch (checkedId){
            case R.id.landEn :
                lang = LANG_EN;
                break;
            default:
                lang = LANG_RU;
        }

        //save
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_LANG_KEY + appWidgetId, lang);
        prefs.commit();
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
        String lang = prefs.getString(PREF_LANG_KEY + appWidgetId, null);
        if (lang != null) {
            return lang;
        } else {
            Resources res = context.getResources();
            Configuration conf = res.getConfiguration();

            return conf.locale.getLanguage();
        }
    }

}
