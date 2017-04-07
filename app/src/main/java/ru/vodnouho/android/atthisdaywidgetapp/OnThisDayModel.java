package ru.vodnouho.android.atthisdaywidgetapp;

import java.util.ArrayList;

/**
 * Created by petukhov on 06.04.2017.
 * неизменяемая модель, которая содержит данные и состояния
 */

public class OnThisDayModel {
    public final boolean isLoading;
    public final boolean isError;
    public final String dayString;
    public final String lang;
    public final ArrayList<Category> categories;

    public OnThisDayModel(String dayString, String lang, ArrayList<Category> categories,
                          boolean isLoading, boolean isError){
        this.dayString = dayString;
        this.lang = lang;
        this.isLoading = isLoading;
        this.isError = isError;
        if(categories != null && categories.size()>0){
            this.categories = new ArrayList<>(categories);
        }else{
            this.categories = null;
        }
    }

}
