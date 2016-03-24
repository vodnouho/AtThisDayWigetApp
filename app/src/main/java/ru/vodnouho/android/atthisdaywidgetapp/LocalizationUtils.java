package ru.vodnouho.android.atthisdaywidgetapp;

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
}
