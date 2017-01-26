package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static android.R.attr.keySet;

/**
 * Created by petukhov on 11.01.2017.
 */

public class NetworkFetcher {
    private static String TAG = "vdnh.NetworkFetcher";
    private static final boolean LOGD = true;
    private static final String CACHE_JSON_FILE_NAME = "cacheJsonSummary";
    private static final String CACHE_BITMAP_FILE_NAME = "cacheJsonBitmaps";
    private static final String CACHE_BITMAP_DIRECTORY_NAME = "cache/bitmaps";
    private static final Object sLock = new Object();

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
        mRequestQueue = getRequestQueue();

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

        //lets find in cache
        Bitmap cachedBitmap = sImageCache.get(url);
        if (cachedBitmap != null) {
            if(LOGD)
                Log.d(TAG, "from cache bitmap:"+url);
            listener.onImageLoaded(url, cachedBitmap);
            return;
        }

        // Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest(
                url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        if(LOGD)
                            Log.d(TAG, "onResponse bitmap:"+url);

                        sImageCache.put(url, bitmap);
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
    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(sContext.getApplicationContext(), new OkHttpStack());
        }
        return mRequestQueue;
    }

    private <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
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
            }
        }

        if(sImageCache != null){
            Map<String, Bitmap> snapshot = sImageCache.snapshot();
            JSONObject jsonObject = new JSONObject();
            int i = 0;
            Iterator<String> iterator = snapshot.keySet().iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                String imageFileName = i + ".jpg";
                Bitmap bitmap = sImageCache.get(key);
                try {
                    saveBitmapToFile(bitmap, imageFileName, sContext);
                    jsonObject.put(key, imageFileName);
                    i++;

                } catch (JSONException e) {
                    Log.e(TAG, "Can't put key:"+key);

                }
            }

            try {
                writeJson(jsonObject, CACHE_BITMAP_FILE_NAME, context);
            } catch (IOException e) {
                Log.e(TAG, "saveCacheToFiles: ", e );
            }
        }

    }

    private void writeJson(JSONObject json, String fileName, Context context) throws IOException {
        Writer w = null;
        OutputStream out;

        try {
            out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            w = new OutputStreamWriter(out);
            w.write(json.toString());
        } finally {
            if (w != null)
                w.close();
        }
    }

    @Nullable
    private JSONObject readJson(String fileName, Context context) throws IOException, JSONException {
        JSONObject result = null;
        BufferedReader reader = null;
        ArrayList<Category> categoryList = new ArrayList<>();

        try {
            InputStream in = context.openFileInput(fileName);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            if (jsonStringBuilder.length() == 0) {
                return null;
            } else {
                result = new JSONObject(jsonStringBuilder.toString());
            }


        } catch (FileNotFoundException e) {
            Log.d(TAG, "Can't find file:" + fileName);

        } finally {
            if (reader != null)
                reader.close();
        }

        return result;
    }


    private void loadCacheFromFiles() {
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
                    Object value = savedJsonCache.get(key);

                    if(value instanceof String) {
                        Bitmap bitmap = loadBitmapFromFile((String) value, sContext);
                        if(key != null && bitmap !=null){
                            sImageCache.put(key, bitmap);
                        }

                        if(LOGD)
                            Log.d(TAG, "loadBitmapCacheFromFiles key:"+key);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't load saved cache", e);
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse saved cache", e);
        }
    }

    public static void saveBitmapToFile(Bitmap bitmap, String fileName, Context context) {
        File imageFile = null;
        OutputStream outputStream = null;
        try {

            File appDir = null;
            appDir = new File(context.getFilesDir(), CACHE_BITMAP_DIRECTORY_NAME);
            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            imageFile = new File(appDir, fileName);

            outputStream = new FileOutputStream(imageFile);

            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();

        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            Log.e(TAG, "Can't save bitmap to file:", e);

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't close file:", e);

                }
            }
        }

    }

    private Bitmap loadBitmapFromFile(String fileName, Context context){
        File appDir = new File(context.getFilesDir(), CACHE_BITMAP_DIRECTORY_NAME);
        File imageFile = new File(appDir, fileName);
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath());
        return bitmap;
    }



    public interface OnLoadListener {
        void onImageLoaded(String url, Bitmap bitmap);

        void onError(String url, Object error);

        void onJsonObjectLoaded(String url, JSONObject jsonResponse);
    }
}
