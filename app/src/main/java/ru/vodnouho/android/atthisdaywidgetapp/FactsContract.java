package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by  on 11.08.2015.
 */
public class FactsContract {
    /**
     * Extra keys for app activity
     */
    public static final String APP_LANG = "ru.vodnouho.android.yourday.APP_LANG";   //"ru" format
    public static final String APP_DATE = "ru.vodnouho.android.yourday.APP_DATE";    //ddmm format
    public static final String APP_CATEGORY_ID = "ru.vodnouho.android.yourday.APP_CATEGORY_ID";
    public static final String APP_CATEGORY_NAME = "ru.vodnouho.android.yourday.APP_CATEGORY_NAME";
    public static final String APP_FACT_ID = "ru.vodnouho.android.yourday.APP_FACT_ID";

    /**
            * The authority of the facts provider.
            */
    public static final String AUTHORITY = "ru.vodnouho.android.yourday.cp";
    public static final Uri CONTENT_URI =   Uri.parse("content://" + AUTHORITY);


    public static class Dates implements BaseColumns{

        public static final String CONTENT_URI_STRING = "dates/*/#";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                FactsContract.CONTENT_URI, CONTENT_URI_STRING);

        /**
         * The mime type of a SINGLE of date.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +
                        "/ru.vodnouho.android.yourday.dates";


        public static final String _ID = BaseColumns._ID;
        public static final String COL_MONTH_DATE = "month_date";
        public static final String COL_LANG = "lang";
    }

    public static class Categories implements BaseColumns{
        public static final String CONTENT_URI_STRING = "categories/*/#"; //  content://" + AUTHORITY + "/categories/ru/0828
        public static final String CONTENT_ITEM_URI_STRING = "categories/#"; //  content://" + AUTHORITY + "/categories/1978

        //The content:// style URL for this table
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                FactsContract.CONTENT_URI, "categories"); //Uri.parse("content://" + AUTHORITY + "/categories/ru/0828");

        public static final Uri CONTENT_ITEM_URI = Uri.withAppendedPath(
                FactsContract.CONTENT_URI, CONTENT_URI_STRING); //Uri.parse("content://" + AUTHORITY + "/categories/ru/0828");

        /**
         * The mime type of a directory of categories.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/ru.vodnouho.android.yourday.categories";

        /**
         * The mime type of a SINGLE of category.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +
                        "/ru.vodnouho.android.yourday.categories";


        public static final String _ID = BaseColumns._ID;
        public static final String NAME = "name"; //text
        public static final String MONTH_DATE_ID = "month_date_id"; //integer

        /**
         * A projection of all columns
         * in the category table.
         */
        public static final String[] PROJECTION_ALL =
                {_ID, NAME, MONTH_DATE_ID};

        /**
         * The default sort order
         */
        public static final String SORT_ORDER_DEFAULT =
                _ID + " ASC";
    }


    public static class Facts implements BaseColumns{
        public static final String FAVCONTENT_URI_STRING = "favfacts/#"; //  content://" + AUTHORITY + "/favfacts/08


        //The content:// style URL for this table
        public static final Uri FAVCONTENT_URI = Uri.withAppendedPath(
                FactsContract.CONTENT_URI, "favfacts"); //Uri.parse("content://" + AUTHORITY + "/categories/ru/0828");


        /**
         * The mime type of a directory of categories.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/ru.vodnouho.android.yourday.facts";

        /**
         * The mime type of a SINGLE of category.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +
                        "/ru.vodnouho.android.yourday.facts";


        public static final String _ID = BaseColumns._ID;
        public static final String TEXT = "text"; //text
        public static final String CATEGORY_ID = "category_id"; //integer
        public static final String IS_FAVORITE = "is_favorite"; //integer 1 if true


        /**
         * A projection of all columns
         * in the category table.
         */
        public static final String[] PROJECTION_ALL =
                {_ID, TEXT, CATEGORY_ID, IS_FAVORITE};

        /**
         * The default sort order
         */
        public static final String SORT_ORDER_DEFAULT =
                _ID + " ASC";
    }




}
