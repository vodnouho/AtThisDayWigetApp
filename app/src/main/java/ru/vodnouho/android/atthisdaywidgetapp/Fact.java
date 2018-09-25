package ru.vodnouho.android.atthisdaywidgetapp;

import androidx.annotation.Nullable;
import android.text.Html;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by petukhov on 01.09.2015.
 */
public class Fact {
    public static final boolean LOGD = false;
    public static final String TAG = "vdnh.Fact";
    public static final String SUMMARY_ENDPOINT = ".m.wikipedia.org/api/rest_v1/page/summary/";
    public static final Pattern PATTERN_ANCHOR = Pattern.compile("<a[^<]+</a>");
    public static final Pattern PATTERN_HREF = Pattern.compile("href=\"(.+)\"");
    public static final Pattern PATTERN_BODY = Pattern.compile(">(.+)</a");

    String id;
    String text;
    private String mLang;

    String mThumbnailUrl;
    private String mYearsAgoString;
    ArrayList<String> mSummaryUrls = new ArrayList<>(3);




    public Fact(String id, String text, String lang) {
        this.id = id;
        this.text = text;
        mLang = lang;

        if (text != null && mLang != null) {
            parseText(text);
        }

    }

    public Fact(JSONObject json){
        try {
            id = json.getString("id");
            text = json.getString("text");
            mLang = json.getString("lang");

            if(!json.isNull("thumbnailUrl")){
                mThumbnailUrl =  json.getString("thumbnailUrl");
            }
            if(!json.isNull("yearsAgoString")){
                mYearsAgoString =  json.getString("yearsAgoString");
            }
            if(!json.isNull("summaryUrls")){
                JSONArray summaryUrls = json.getJSONArray("summaryUrls");
                mSummaryUrls = new ArrayList<String>();
                for(int i=0; i<summaryUrls.length();i++){
                    mSummaryUrls.add(summaryUrls.getString(i)) ;
                }

            }
        } catch (JSONException e) {
            Log.e(TAG, "can't create Fact from JSON", e);
        }
    }

    public JSONObject toJSON(){
        JSONObject result = new JSONObject();
        try {
            result.put("id", id);
            result.put("text", text);
            result.put("lang", mLang);
            if(mThumbnailUrl != null){
                result.put("thumbnailUrl", mThumbnailUrl);
            }
            if(mYearsAgoString != null){
                result.put("yearsAgoString", mYearsAgoString);
            }
            if(mSummaryUrls != null && mSummaryUrls.size() > 0){
                JSONArray summaryUrls = new JSONArray(mSummaryUrls);
                result.put("summaryUrls", summaryUrls);
            }
        } catch (JSONException e) {
            Log.e(TAG, "can't convert Fact to JSON", e);
            return null;
        }
        return result;
    }

    private void parseText(String text) {
        if (LOGD)
            Log.d(TAG, "parseText : " + text);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Matcher anchorMatcher = PATTERN_ANCHOR.matcher(text);

        int anchorCount = 0;
        while (anchorMatcher.find()) {
            //for all <a href="https://ru.m.wikipedia.org/wiki/2007 год">2007</a>

            anchorCount++;

            String aTag = anchorMatcher.group();
            Matcher hrefMatcher = PATTERN_HREF.matcher(aTag);
            Matcher bodyMatcher = PATTERN_BODY.matcher(aTag);
            if (hrefMatcher.find() && bodyMatcher.find()) {

                String href = hrefMatcher.group().substring(6, hrefMatcher.group().length() - 1);
                String wikiTitle = href.substring(href.lastIndexOf("/") + 1);
                String body = bodyMatcher.group().substring(1, bodyMatcher.group().length() - 3);

                Log.d(TAG, "match a:" + aTag
                        + " href:" + href
                        + " wikiTitle:" + wikiTitle
                        + " body:" + body);


                if (isNumeric(body)) {
                    if (anchorCount == 1) {
                        //dated event
                        int eventYear = Integer.parseInt(body);
                        mYearsAgoString = String.valueOf((currentYear - eventYear));
                    }

                    continue; //we no need year picture
                }

                //get lang from link https://ru.m.wikipedia.org/wiki/2001
                int indexFirstDot = href.indexOf(".");
                String linkLang = mLang;
                if (indexFirstDot > 2) {
                    linkLang = href.substring(indexFirstDot - 2, indexFirstDot);
                }

                //replace spaces by underscore
                if(wikiTitle.contains(" ")){
                    wikiTitle = wikiTitle.replace(" ", "_");
                }


                try {
                    if (linkLang == null) {
                        throw new IllegalStateException("Lang is not setted.");
                    }

                    mSummaryUrls.add("https://"
                                    + linkLang
                                    + SUMMARY_ENDPOINT
                                    + URLEncoder.encode(wikiTitle, "UTF-8")
                            //+ titleSubstring
                    );
                } catch (Throwable e) {
                    Log.d(TAG, "Can't add summary endpoint.", e);
                }

            }

        }
    }

    private void findYearsAgoString(String text) {
        if (text == null) {
            return;
        }

        Log.d(TAG, "findYearsAgoString: " + text);
        String plain = Html.fromHtml(text).toString();
        Log.d(TAG, "plain: " + plain);
        String[] splitedStrings = plain.split("\\s+");
        if (splitedStrings.length > 0) {
            String firstSub = splitedStrings[0];
            if (firstSub.endsWith(":")) {
                firstSub = firstSub.replace(":", "");
            }
            if (isNumeric(firstSub)) {
                int eventYear = Integer.parseInt(firstSub);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                //once get localized MORE string

                mYearsAgoString = String.valueOf((currentYear - eventYear));
            }
        }

    }

    public static boolean isNumeric(String str) {
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
        if (mSummaryUrls == null || mSummaryUrls.size() == 0) {
            return null;
        } else {
            return mSummaryUrls.get(0);
        }
    }


    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    /**
     * Set the thumbnailUrl or remove title without thumbnail from queue
     *
     * @param thumbnailUrl - set URL of image or "" for remove title
     * @param url          - title url
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
