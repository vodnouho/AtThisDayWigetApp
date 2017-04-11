package ru.vodnouho.android.atthisdaywidgetapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public OnThisDayModel(JSONObject jsonObject) throws JSONException {
        dayString = jsonObject.getString("dayString");
        lang = jsonObject.getString("lang");
        this.isLoading = jsonObject.getBoolean("isLoading");
        this.isError = jsonObject.getBoolean("isError");
        if(!jsonObject.isNull("categories")){
            JSONArray categoriesJsonArray = jsonObject.getJSONArray("categories");
            categories = new ArrayList<>(categoriesJsonArray.length());

            for(int i=0; i<categoriesJsonArray.length();i++){
                JSONObject jsonCategory = categoriesJsonArray.getJSONObject(i);
                categories.add(new Category(jsonCategory));
            }
        }else{
            this.categories = null;
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("dayString", dayString);
        result.put("lang", lang);
        result.put("isLoading", isLoading);
        result.put("isError", isError);
        if(categories != null){
            JSONArray categoriesJsonArray = new JSONArray();
            for(int i=0; i< categories.size(); i++){
                JSONObject categoryJson =  (categories.get(i)).toJSON();
                categoriesJsonArray.put(categoryJson);
            }

            result.put("categories", categoriesJsonArray);
        }
        return result;
    }
}
