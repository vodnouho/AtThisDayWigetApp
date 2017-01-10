package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
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

    /**
     * Filter lang which localization exist
     * @param lang - want to
     * @return lang if localization exist or default language (en)
     */
    public static String restrictLanguage(String lang){
        if(LANG_RU.equals(lang)){
            return lang;
        }else if(LANG_ES.equals(lang)){
            return lang;
        }else if(LANG_DE.equals(lang)){
            return lang;
//        }else if(FactLab.LANG_ZH.equals(lang)){
//            return;
        }else if(LANG_FR.equals(lang)){
            return lang;
        }else{
            return LANG_EN;
        }

    }

    public static String createLocalizedTitle(Context context, Date date){
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
        }else if (LANG_ES.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(" de ");
            titleText.append(monthNames[month].toLowerCase());

        }else if (LANG_DE.equals(lang)) {
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(". ");
            titleText.append(monthNames[month]);
/*
        }else if (FactLab.LANG_ZH.equals(mLang)) { //3月5日
            return month+"月"+ day + "日";
*/
        }else if (LANG_FR.equals(lang)) {
            String article = " ";
            if(dayOfMonth == 1){
                article="er ";
            }
            titleText.append(" ");
            titleText.append(dayOfMonth);
            titleText.append(article);
            titleText.append(monthNames[month].toLowerCase());

        }else{
            //like english by default
            titleText.append(" ");
            titleText.append(monthNames[month]);
            titleText.append(" ");
            titleText.append(dayOfMonth);
        }


        return titleText.toString();

    }

    public static void setLocate(Context context, String lang) {
        Locale newLocale;

        if (LANG_RU.equals(lang)) {
            newLocale = new Locale(LANG_RU, "RU");
        } else if(LANG_ES.equals(lang)) {
            newLocale = new Locale(LANG_ES, "ES");
        } else if(LANG_DE.equals(lang)) {
            newLocale = new Locale(LANG_DE, "DE");
        } else if(LANG_FR.equals(lang)) {
                newLocale = new Locale(LANG_FR, "FR");
        } else {
            newLocale = Locale.US;
        }


        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = newLocale;
        res.updateConfiguration(conf, dm);

    }

    public static String getLocalizedString(int stringId, String lang, Context context){
        String result = null;
        lang = restrictLanguage(lang);

        //save current Lang
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        String currentLang = conf.locale.getLanguage();

        boolean isLangChaged = false;
        if (!lang.equals(currentLang)){
            LocalizationUtils.setLocate(context, lang);
            isLangChaged = true;
        }
        result = context.getString(stringId);
        if(isLangChaged){
            LocalizationUtils.setLocate(context, currentLang);
        }


        return result;
    }

}
