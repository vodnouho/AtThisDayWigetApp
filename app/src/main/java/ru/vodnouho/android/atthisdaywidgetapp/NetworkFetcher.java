package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by petukhov on 11.01.2017.
 */

public class NetworkFetcher {
    private static String TAG = "vdnh.NetworkFetcher";
    private static final boolean LOGD = true;

    private static NetworkFetcher sInstance;
    private static Context sContext;

    private static LruCache<String, JSONObject> sSummaryCache = new LruCache<String, JSONObject>(84); //title is key, path is value
    private static LruCache<String, Bitmap> sImageCache = new LruCache<String, Bitmap>(21); //title is key, path is value

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;


    public static synchronized NetworkFetcher getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NetworkFetcher(context.getApplicationContext());
        }
        return sInstance;
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
     * @param url - URL of Image
     * @param listener - Listener
     */
    public void requestImage(final String url, final OnLoadListener listener) {

        //lets find in cache
        Bitmap cachedBitmap = sImageCache.get(url);
        if(cachedBitmap != null){
            listener.onImageLoaded(url, cachedBitmap);
            return;
        }

        // Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest(
                url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        sImageCache.put(url, bitmap);
                        listener.onImageLoaded(url, bitmap);
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
     * @param url - URL of Image
     * @param listener - Listener
     */
    public void requestJsonObject(final String url, final OnLoadListener listener) {
        //lets find in cache
        JSONObject cachedSummary = sSummaryCache.get(url);
        if(cachedSummary != null){
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
     * @return
     */
    private ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public void cancelAll(OnLoadListener listener) {
        String tag = listener.toString();
        mRequestQueue.cancelAll(tag);
    }

    public interface OnLoadListener {
        void onImageLoaded(String url, Bitmap bitmap);
        void onError(String url, Object error);
        void onJsonObjectLoaded(String url, JSONObject jsonResponse);
    }
}
