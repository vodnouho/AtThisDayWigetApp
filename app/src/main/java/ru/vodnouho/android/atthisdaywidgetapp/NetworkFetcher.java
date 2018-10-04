package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.collection.LruCache;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import static ru.vodnouho.android.atthisdaywidgetapp.BuildConfig.DEBUG;
import static ru.vodnouho.android.atthisdaywidgetapp.SaveLoadHelper.readJson;
import static ru.vodnouho.android.atthisdaywidgetapp.SaveLoadHelper.writeJson;

/**
 * Created by petukhov on 11.01.2017.
 */

public class NetworkFetcher {
    private static String TAG = "vdnh.NetworkFetcher";
    private static final boolean LOGD = true;
    private static final String CACHE_JSON_FILE_NAME = "cacheJsonSummary";
    private static final String CACHE_BITMAP_FILE_NAME = "cacheJsonBitmaps";
    private static final String CACHE_BITMAP_DIRECTORY_NAME = "cache/bitmaps";
    private static final String CACHE_VOLLEY_DIRECTORY_NAME = "cache/volley";
    private static final String CACHE_BITMAP_EXTENSION = ".png";
    private static final Object sLock = new Object();
    private static final long TIME_TO_LIVE_MILLIS = 1000 * 60 * 60 * 24 * 2; //two days





    private static NetworkFetcher sInstance;
    private static Context sContext;


    private static boolean isNewImageLoaded = false;

    private static LruCache<String, JSONObject> sSummaryCache = new LruCache<String, JSONObject>(84); //title is key, path is value
    private static LruCache<String, Bitmap> sImageCache = new LruCache<String, Bitmap>(21); //title is key, path is value

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;




    public static synchronized NetworkFetcher getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NetworkFetcher(context.getApplicationContext());
            sInstance.loadCacheFromFiles();
            sInstance.deleteOldBitmapFiles(context.getApplicationContext());
        }
        return sInstance;
    }



    public static void saveState() {
        if (LOGD)
            Log.d(TAG, "saveState");


        if (isNewImageLoaded && sInstance != null && sContext != null) {
            synchronized (sLock){
                sInstance.saveCacheToFiles(sContext);
            }
            isNewImageLoaded = false;
        }
    }

    private NetworkFetcher(Context applicationContext) {
        sContext = applicationContext;
        mRequestQueue = getRequestQueue(applicationContext);

        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }


    /**
     * Used for download image and return it to listener.
     *
     * @param url      - URL of Image
     * @param listener - Listener
     */
    public void requestImage(final String url, final OnLoadListener listener) {
        if(LOGD)
            Log.d(TAG, "requestImage bitmap:"+url);
        //lets find in cache
        Bitmap cachedBitmap = sImageCache.get(url);
        if (cachedBitmap != null) {
            if(LOGD){
                Log.d(TAG, "from cache bitmap:"+url);
            }
            listener.onImageLoaded(url, cachedBitmap);
            return;
        }

        // Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest(
                url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        if(LOGD){
                            Log.d(TAG, "onResponse bitmap:"+url);
                        }
                        Bitmap shadow = Utils.addShadow(bitmap,
                                bitmap.getHeight(),
                                bitmap.getWidth(),
                                Color.BLACK,
                                3,1,1);
                       // bitmap.recycle();

                        sImageCache.put(url, shadow);
                        listener.onImageLoaded(url, bitmap);
                        isNewImageLoaded = true;
                    }
                }
                , 90, 90, null, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        listener.onError(url, error);
                    }
                });

        request.setRetryPolicy(new DefaultRetryPolicy(7500, 3, 1f));
        // Access the RequestQueue through your singleton class.
        addToRequestQueue(request);
    }

    /**
     * Used for download JSON object and return it to listener.
     *
     * @param url      - URL of Image
     * @param listener - Listener
     */
    public void requestJsonObject(final String url, final OnLoadListener listener) {
        //lets find in cache
        JSONObject cachedSummary = sSummaryCache.get(url);
        if (cachedSummary != null) {
            listener.onJsonObjectLoaded(url, cachedSummary);
            return;
        }


        JsonObjectRequest request = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        sSummaryCache.put(url, response);
                        listener.onJsonObjectLoaded(url, response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        listener.onError(url, error);
                    }
                });

        request.setRetryPolicy(new DefaultRetryPolicy(7500, 3, 1f));
        // Access the RequestQueue through your singleton class.
        addToRequestQueue(request);
    }

    /**
     * public ???
     *
     * @return
     */
    private RequestQueue getRequestQueue(Context context) {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(sContext.getApplicationContext()); //deprecated
        }
        return mRequestQueue;
    }

    private <T> void addToRequestQueue(Request<T> req) {


        getRequestQueue(sContext).add(req);
    }

    /**
     * not used yet, may be later
     *
     * @return
     */
    private ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public void cancelAll(OnLoadListener listener) {
        String tag = listener.toString();
        mRequestQueue.cancelAll(tag);
    }


    private void saveCacheToFiles(Context context) {
        if (LOGD)
            Log.d(TAG, "saveCacheToFiles ");


        if (sSummaryCache != null) {
            Map<String, JSONObject> snapshot = sSummaryCache.snapshot();
            JSONObject jsonObject = new JSONObject(snapshot);
            try {
                writeJson(jsonObject, CACHE_JSON_FILE_NAME, context);
            } catch (IOException e) {
                Log.e(TAG, "Can't write json:" + jsonObject.toString());
                context.deleteFile(CACHE_JSON_FILE_NAME);
                return;
            }
        }


        boolean isFilesWroteCorrectly = true;
        if(sImageCache != null){
            Map<String, Bitmap> snapshot = sImageCache.snapshot();
            JSONObject jsonObject = new JSONObject();
            //int i = 0;
            for (String key : snapshot.keySet()) {
                String imageFileName = Utils.getMD5EncryptedString(key) + CACHE_BITMAP_EXTENSION;
                Bitmap bitmap = sImageCache.get(key);
                try {
                    if (bitmap != null) {
                        saveBitmapToFile(bitmap, imageFileName, sContext);
                        jsonObject.put(key, imageFileName);
                    }

                    //i++;

                } catch (Throwable e) {
                    Log.e(TAG, "Can't put key:" + key);

                    isFilesWroteCorrectly = false;
                }
            }


            try {
                writeJson(jsonObject, CACHE_BITMAP_FILE_NAME, context);
            } catch (IOException e) {
                Log.e(TAG, "saveCacheToFiles: ", e );
                isFilesWroteCorrectly = false;
            }

            if(!isFilesWroteCorrectly){
                //clear all imageFiles
/*
                for(;i>=0;i--){
                    deleteBitmapFile(i + CACHE_BITMAP_EXTENSION, sContext);
                }
*/
                context.deleteFile(CACHE_BITMAP_FILE_NAME);
            }
        }

    }




    private static synchronized void loadCacheFromFiles() {
        //load summaries

        try {
            JSONObject savedJsonCache = readJson(CACHE_JSON_FILE_NAME, sContext);
            if(savedJsonCache != null){
                Iterator<String> keysItr = savedJsonCache.keys();
                while(keysItr.hasNext()) {
                    String key = keysItr.next();
                    Object value = savedJsonCache.get(key);

                    if(value instanceof JSONObject) {
                        sSummaryCache.put(key, (JSONObject)value);
                        if(LOGD)
                            Log.d(TAG, "loadCacheFromFiles key:"+key);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't load saved cache", e);
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse saved cache", e);
        }



        //load bitmaps
        try {
            JSONObject savedJsonCache = readJson(CACHE_BITMAP_FILE_NAME, sContext);
            if(savedJsonCache != null){
                Iterator<String> keysItr = savedJsonCache.keys();
                while(keysItr.hasNext()) {
                    String key = keysItr.next();
                    //Object value = savedJsonCache.get(key);
                    String value = Utils.getMD5EncryptedString(key) + CACHE_BITMAP_EXTENSION;



                        Bitmap bitmap = loadBitmapFromFile((String) value, sContext);
                        if(key != null && bitmap !=null){
                            sImageCache.put(key, bitmap);
                        }

                        if(LOGD)
                            Log.d(TAG, "loadBitmapCacheFromFiles key:"+key);

                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't load saved cache", e);
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse saved cache", e);
        }
    }

    public synchronized static void deleteBitmapFile(String fileName, Context context){
        File appDir = null;
        appDir = new File(context.getFilesDir(), CACHE_BITMAP_DIRECTORY_NAME);
        if (!appDir.exists()) {
            //no dirs - no file
           return;
        }

        File deletingFile = new File(appDir, fileName);
        deletingFile.delete();
    }

    private void deleteOldBitmapFiles(Context context) {
        File appDir = null;
        appDir = new File(context.getFilesDir(), CACHE_BITMAP_DIRECTORY_NAME);
        if (!appDir.exists()) {
            //no dirs - no file
            return;
        }

        String[] fileNames = appDir.list();
        if(fileNames != null){
            long oldDate = System.currentTimeMillis() - TIME_TO_LIVE_MILLIS;
            for (String fileName: fileNames ) {
                File candidate = new File(appDir, fileName);
                if(candidate.lastModified() < oldDate){
                    candidate.delete();
                    if(DEBUG) Log.d(TAG, "File:" + fileName + " deleted");
                }
            }
        }

    }

    public synchronized static void saveBitmapToFile(Bitmap bitmap, String fileName, Context context) throws Throwable{
        Throwable error = null;
        File imageFile = null;
        OutputStream outputStream = null;
        try {

            File appDir = null;
            appDir = new File(context.getFilesDir(), CACHE_BITMAP_DIRECTORY_NAME);
            if (!appDir.exists()) {
                appDir.mkdirs();
            }



            imageFile = new File(appDir, fileName);

            if(imageFile.exists()){
                return;
            }

            outputStream = new FileOutputStream(imageFile);

            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream);
            outputStream.flush();

        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            Log.e(TAG, "Can't save bitmap to file:", e);
            error = e;

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't close file:", e);
                    error = e;
                }
            }
        }

        if(error != null){
            throw error;
        }

    }

    private static Bitmap loadBitmapFromFile(String fileName, Context context){
        File appDir = new File(context.getFilesDir(), CACHE_BITMAP_DIRECTORY_NAME);
        File imageFile = new File(appDir, fileName);
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath());
        return bitmap;
    }

    public static Bitmap getCachedBitmap(String thumbnailUrl) {
        Bitmap bitmap = sImageCache.get(thumbnailUrl);
        if(DEBUG)
            Log.d(TAG, "bitmap from cache:"+bitmap);
        return bitmap;
    }


    public interface OnLoadListener {
        void onImageLoaded(String url, Bitmap bitmap);

        void onError(String url, Object error);

        void onJsonObjectLoaded(String url, JSONObject jsonResponse);
    }
}
