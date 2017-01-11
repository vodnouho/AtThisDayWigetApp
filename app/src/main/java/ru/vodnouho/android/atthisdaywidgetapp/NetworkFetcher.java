package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by petukhov on 11.01.2017.
 */

public class NetworkFetcher {
    private static String TAG = "vdnh.NetworkFetcher";
    private static final boolean LOGD = true;

    private static NetworkFetcher sInstance;
    private static Context sContext;
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


    public void requestImage(final String url, final OnLoadListener listener) {

        // Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest(
                url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        listener.onImageLoaded(url, bitmap);
                    }
                }
                , 0, 0, null, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        listener.onError(url, error);
                    }
                });
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
            mRequestQueue = Volley.newRequestQueue(sContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    private <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    private ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public void cancelAll(OnLoadListener listener) {
        String tag = listener.toString();
        mRequestQueue.cancelAll(tag);
    }

    public interface OnLoadListener {
        public void onImageLoaded(String url, Bitmap bitmap);
        public void onError(String url, Object error);
    }
}
