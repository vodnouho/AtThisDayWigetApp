package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.DisplayMetrics;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by petukhov on 24.03.2016.
 */
public class LocalizationUtils {
    public static final String LANG_RU = "ru";
    public static final String LANG_ES = "es";
    public static final String LANG_DE = "de";
    public static final String LANG_FR = "fr";
    public static final String LANG_EN = "en";
    public static final String LANG_PT = "pt";

    /**
     * Filter lang which localization exist
     *
     * @param lang - want to
     * @return lang if localization exist or default language (en)
     */
    public static String restrictLanguage(String lang) {
        if (LANG_RU.equals(lang)) {
            return lang;
        } else if (LANG_ES.equals(lang)) {
            return lang;
        } else if (LANG_DE.equals(lang)) {
            return lang;
//        }else if(FactLab.LANG_ZH.equals(lang)){
//            return;
        } else if (LANG_FR.equals(lang)) {
            return lang;
        } else if (LANG_PT.equals(lang)) {
            return lang;
        } else {
            return LANG_EN;
        }

    }


    public static String createLocalizedTitle(Context context, String lang, Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);

        Resources res = null;
        String[] monthNames = null;
        String title;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            res = getLocalizedResources(context, getLocale(lang));
            monthNames = res.getStringArray(R.array.month_names);
            title = res.getString(R.string.title);
        } else {

            res = context.getResources();
            Configuration conf = res.getConfiguration();
            String currentLang = conf.locale.getLanguage();

            boolean isLangChanged = false;
            if (!currentLang.equals(lang)) {
                LocalizationUtils.setLocate(context, lang);
                isLangChanged = true;
            }

            monthNames = res.getStringArray(R.array.month_names);
            title = res.getString(R.string.title);

            if (isLangChanged) {
                LocalizationUtils.setLocate(context, currentLang);
            }

        }


        StringBuilder titleText = new StringBuilder();
        titleText.append(title);


        String localizedMonths = monthNames[month];
        if (LANG_RU.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(" ");
            titleText.append(monthNames[month]);
        } else if (LANG_EN.equals(lang)) {
            titleText.append(" ");
            titleText.append(monthNames[month]);
            titleText.append(" ");
            titleText.append(dayOfMonth);
        } else if (LANG_ES.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(" de ");
            titleText.append(monthNames[month].toLowerCase());
        } else if (LANG_PT.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(" de ");
            titleText.append(monthNames[month].toLowerCase());
        } else if (LANG_DE.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(". ");
            titleText.append(monthNames[month]);
/*
        }else if (FactLab.LANG_ZH.equals(mLang)) { //3月5日
            return month+"月"+ day + "日";
*/
        } else if (LANG_FR.equals(lang)) {
            String article = " ";
            if (dayOfMonth == 1) {
                article = "er ";
            }
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(article);
            titleText.append(monthNames[month].toLowerCase());

        } else {
            //like english by default
            titleText.append(" ");
            titleText.append(monthNames[month]);
            titleText.append(" ");
            titleText.append(dayOfMonth);
        }


        return titleText.toString();
    }

    public static String createLocalizedTitle(Context context, Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);

        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String lang = conf.locale.getLanguage();
        String[] monthNames = res.getStringArray(R.array.month_names);

        StringBuilder titleText = new StringBuilder();
        titleText.append(res.getString(R.string.title));


        String localizedMonths = monthNames[month];
        if (LANG_RU.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(" ");
            titleText.append(monthNames[month]);
        } else if (LANG_EN.equals(lang)) {
            titleText.append(" ");
            titleText.append(monthNames[month]);
            titleText.append(" ");
            titleText.append(dayOfMonth);
        } else if (LANG_ES.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(" de ");
            titleText.append(monthNames[month].toLowerCase());
        } else if (LANG_PT.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(" de ");
            titleText.append(monthNames[month].toLowerCase());
        } else if (LANG_DE.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(". ");
            titleText.append(monthNames[month]);
/*
        }else if (FactLab.LANG_ZH.equals(mLang)) { //3月5日
            return month+"月"+ day + "日";
*/
        } else if (LANG_FR.equals(lang)) {
            String article = " ";
            if (dayOfMonth == 1) {
                article = "er ";
            }
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(article);
            titleText.append(monthNames[month].toLowerCase());

        } else {
            //like english by default
            titleText.append(" ");
            titleText.append(monthNames[month]);
            titleText.append(" ");
            titleText.append(dayOfMonth);
        }


        return titleText.toString();

    }

    public static void setLocate(Context context, String lang) {
        Locale newLocale = getLocale(lang);

/*
        if (LANG_RU.equals(lang)) {
            newLocale = new Locale(LANG_RU, "RU");
        } else if (LANG_ES.equals(lang)) {
            newLocale = new Locale(LANG_ES, "ES");
        } else if (LANG_DE.equals(lang)) {
            newLocale = new Locale(LANG_DE, "DE");
        } else if (LANG_FR.equals(lang)) {
            newLocale = new Locale(LANG_FR, "FR");
        } else if (LANG_PT.equals(lang)) {
            newLocale = new Locale(LANG_PT, "FR");
        } else {
            newLocale = Locale.US;
        }
*/


        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = newLocale;
        res.updateConfiguration(conf, dm);

    }


    public static Locale getLocale(String lang) {
        Locale newLocale;
        if (LANG_RU.equals(lang)) {
            newLocale = new Locale(LANG_RU, "RU");
        } else if (LANG_ES.equals(lang)) {
            newLocale = new Locale(LANG_ES, "ES");
        } else if (LANG_DE.equals(lang)) {
            newLocale = new Locale(LANG_DE, "DE");
        } else if (LANG_FR.equals(lang)) {
            newLocale = new Locale(LANG_FR, "FR");
        } else if (LANG_PT.equals(lang)) {
            newLocale = new Locale(LANG_PT, "FR");
        } else {
            newLocale = Locale.US;
        }
        return newLocale;
    }

    public static String getLocalizedString(int stringId, String lang, Context context, Object... formatArgs) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Resources localizedResources = getLocalizedResources(context, getLocale(lang));
            return localizedResources.getString(stringId, formatArgs);
        } else {
            return getLocalizedStringOld(stringId, lang, context, formatArgs);
        }
    }

    public static String getLocalizedStringOld(int stringId, String lang, Context context, Object... formatArgs) {
        String result = null;
        lang = restrictLanguage(lang);

        //save current Lang
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();

        boolean isLangChaged = false;
        if (!lang.equals(currentLang)) {
            LocalizationUtils.setLocate(context, lang);
            isLangChaged = true;
        }
        result = context.getString(stringId, formatArgs);
        //result = context.getString(stringId);
        if (isLangChaged) {
            LocalizationUtils.setLocate(context, currentLang);
        }


        return result;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @NonNull
    public static Resources getLocalizedResources(Context context, Locale desiredLocale) {
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = null;
        localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }

}
