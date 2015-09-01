package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * Created by petukhov on 01.09.2015.
 */
public class OTDRemoteViews {
    private static String TAG = "OTDRemoteViews";
    private RemoteViews mViews;

    public OTDRemoteViews(Context context, String titleText, ArrayList<Category> categories) {

        // Construct the RemoteViews object.
        mViews = new RemoteViews(context.getPackageName(), R.layout.atd_widget_layout);
        mViews.setTextViewText(R.id.titleTextView, titleText);

        for(Category c: categories){

            Log.d(TAG, "Category:"+c.name);
            ArrayList<Fact> facts = c.getFavFacts();
            for (Fact f : facts){
                Log.d(TAG, "Fatc:"+f.text);
            }
        }


    }

    public RemoteViews getViews() {



        return mViews;
    }
}
