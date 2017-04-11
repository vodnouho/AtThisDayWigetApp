package ru.vodnouho.android.atthisdaywidgetapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by petukhov on 01.09.2015.
 */
public class Category {
    public String name;
    public String id;
    private ArrayList<Fact> mFacts;

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Category(JSONObject jsonCategory) throws JSONException {
        name = jsonCategory.getString("name");
        id = jsonCategory.getString("id");

        if (!jsonCategory.isNull("facts")) {
            JSONArray factsJsonArray = jsonCategory.getJSONArray("facts");
            mFacts = new ArrayList<>(factsJsonArray.length());

            for(int i=0; i<factsJsonArray.length(); i++){
                JSONObject factJsonObject = factsJsonArray.getJSONObject(i);
                Fact fact = new Fact(factJsonObject);
                mFacts.add(fact);
            }
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("name", name);
        result.put("id", id);
        if(mFacts != null && mFacts.size()>0){
            JSONArray factsJsonArray = new JSONArray();
            for(int i=0; i<mFacts.size(); i++){
                factsJsonArray.put(mFacts.get(i).toJSON());
            }
            result.put("facts", factsJsonArray);
        }
        return result;
    }


    public void add(Fact fact) {
        if (fact == null) {
            return;
        }

        if (mFacts == null) {
            mFacts = new ArrayList<>(3);
        }

        mFacts.add(fact);
    }

    public ArrayList<Fact> getFavFacts() {
        return mFacts;
    }

    public void clearFavFacts() {
        if (mFacts != null) {
            mFacts.clear();
        }
    }

    public void removeFavFact(Fact f) {
        if (mFacts != null) {
            mFacts.remove(f);
        }

    }

}
