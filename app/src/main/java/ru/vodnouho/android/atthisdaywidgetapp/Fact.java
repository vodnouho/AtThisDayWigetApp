package ru.vodnouho.android.atthisdaywidgetapp;

import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by petukhov on 01.09.2015.
 */
public class Fact {
    public static final boolean LOGD = true;
    public static final String TAG = "vdnh.Fact";
    public static final String SUMMARY_ENDPOINT = ".m.wikipedia.org/api/rest_v1/page/summary/";

    String id;
    String text;
    String mThumbnailUrl;
    ArrayList<String> mSummaryUrls = new ArrayList<>(3);
    private String mLang;
    private String mYearsAgoString;


    public Fact(String id, String text, String lang) {
        this.id = id;
        this.text = text;
        mLang = lang;

        if (text != null && mLang != null) {
            parseText(text);
        }

    }

    private void parseText(String text) {
        if(LOGD)
            Log.d(TAG, "parseText : "+text);

        findYearsAgoString(text);

        int indexHrefStart = text.indexOf("href=\"");
        while (indexHrefStart > -1) {
            indexHrefStart += "href=\"".length();
            int indexHrefEnd = text.indexOf("\"", indexHrefStart);
            if (indexHrefEnd == -1) {
                break;
            }

            int indexFirstDot = text.indexOf(".", indexHrefStart);
            String linkLang = mLang;
            if(indexFirstDot > 2){
                linkLang = text.substring(indexFirstDot-2, indexFirstDot);
            }


            int indexSlash = text.lastIndexOf("/", indexHrefEnd);
            if (indexSlash == -1) {
                break;
            }else{
                indexSlash++;
            }

            String titleSubstring = text.substring(indexSlash, indexHrefEnd);

            //if text start with number link it is a year of event. Ignore it.
            if(isNumeric(titleSubstring.trim())){
                indexHrefStart = text.indexOf("href=\"", indexHrefEnd);
                continue;
            }

            titleSubstring = titleSubstring.replace(" ", "_");

            try {
                if(mLang == null){
                    throw new IllegalStateException("Lang is not setted.");
                }

                mSummaryUrls.add("https://"
                        + linkLang
                        + SUMMARY_ENDPOINT
                        + URLEncoder.encode(titleSubstring, "UTF-8")
                        //+ titleSubstring
                );
            } catch (Throwable e) {
                Log.d(TAG, "Can't add summary endpoint.", e);
            }

            indexHrefStart = text.indexOf("href=\"", indexHrefEnd);
        }
    }

    private void findYearsAgoString(String text) {
        if(text == null){
            return;
        }

        Log.d(TAG, "findYearsAgoString: "+ text);
        String plain = Html.fromHtml(text).toString();
        Log.d(TAG, "plain: "+plain);
        String[] splitedStrings = plain.split("\\s+");
        if(splitedStrings.length > 0 ){
            String firstSub = splitedStrings[0];
            if(firstSub.endsWith(":")){
                firstSub = firstSub.replace(":","");
            }
            if(isNumeric(firstSub)){
                int eventYear = Integer.parseInt(firstSub);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                //once get localized MORE string

                mYearsAgoString = String.valueOf((currentYear - eventYear));
            }
        }

    }

    public static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    /**
     * Check is fact may has picture (has unchecked links)
     *
     * @return
     */
    public boolean mayHasThumbnail() {
        if (mSummaryUrls != null && !mSummaryUrls.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * return array of summary URLs to find a picture URL
     *
     * @return
     */
    public ArrayList<String> getTitlesForPicture() {
        return mSummaryUrls;
    }

    @Nullable
    public String getTitleForPicture() {
        if(mSummaryUrls == null || mSummaryUrls.size() == 0){
            return null;
        }else{
            return mSummaryUrls.get(0);
        }
    }


    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    /**
     * Set the thumbnailUrl or remove title without thumbnail from queue
     * @param thumbnailUrl - set URL of image or "" for remove title
     * @param url - title url
     */
    public void setThumbnailUrl(String thumbnailUrl, String url) {
        if (thumbnailUrl != null) {
            if (thumbnailUrl.isEmpty() && url != null) {
                //remove title summary url from queue
                mSummaryUrls.remove(url);
            } else {
                mSummaryUrls.clear();
                mThumbnailUrl = thumbnailUrl;
            }
        }
    }

    public void setLang(String lang) {
        mLang = lang;
    }

    public String getYearsAgoString() {
        return mYearsAgoString;
    }
}
