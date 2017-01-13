package ru.vodnouho.android.atthisdaywidgetapp;

import java.util.ArrayList;

/**
 * Created by petukhov on 01.09.2015.
 */
public class Category {
    public String name;
    public String id;
    private ArrayList<Fact> mFacts;

    public Category(String id, String name){
        this.id = id;
        this.name = name;
    }

    public void add(Fact fact) {
        if(fact == null){
            return;
        }

        if(mFacts == null){
            mFacts = new ArrayList<>(3);
        }

        mFacts.add(fact);
    }

    public ArrayList<Fact> getFavFacts() {
        return mFacts;
    }

    public void clearFavFacts() {
        if(mFacts != null){
            mFacts.clear();
        }
    }
}
