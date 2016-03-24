package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * Created by petukhov on 01.09.2015.
 */
public class OTDRemoteViews {
    private static String TAG = "OTDRemoteViews";
    private RemoteViews mViews;

    public OTDRemoteViews(Context context, String titleText, ArrayList<Category> categories, int widgetId) {

        // Construct the RemoteViews object.
        mViews = new RemoteViews(context.getPackageName(), R.layout.atd_widget_layout);
        mViews.setTextViewText(R.id.titleTextView, titleText);

        Intent adapter = new Intent(context, CategoryListRemoteViewsFactory.class);
        adapter.putExtra(UpdateService.EXTRA_WIDGET_ID, widgetId);
        Uri data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME));
        adapter.setData(data);
        mViews.setRemoteAdapter(R.id.listView, adapter);


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
