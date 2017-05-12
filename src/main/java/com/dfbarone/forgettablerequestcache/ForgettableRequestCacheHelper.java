package com.dfbarone.forgettablerequestcache;

import android.content.Context;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.dfbarone.forgettablerequestcache.request.CacheHeaderInterceptor;
import com.dfbarone.forgettablerequestcache.request.HeaderInterceptorJsonObjectRequest;
import com.dfbarone.forgettablerequestcache.request.ForgettableRequestCacheInterceptor;

import org.json.JSONObject;

/**
 * Created by dbarone on 5/12/2017.
 */

public class ForgettableRequestCacheHelper implements CacheHeaderInterceptor {

    private final String TAG = ForgettableRequestCacheHelper.class.getName();

    protected static RequestQueue mRequestQueue;
    protected ForgettableRequestCacheInterceptor mInterceptor = new ForgettableRequestCacheInterceptor();

    private static ForgettableRequestCacheHelper mInstance;
    public static ForgettableRequestCacheHelper getInstance() {
        if (mInstance == null) {
            mInstance = new ForgettableRequestCacheHelper();
        }
        return mInstance;
    }

    public static void init(Context context) {
        if (mRequestQueue == null) {
            mRequestQueue = ForgettableVolley.newRequestQueue(context);
        }
    }

    // Get method
    public void get(final String url, final VolleyCallback callback) {
        request(Request.Method.GET, url, null, callback, this);
    }

    // Post method
    public void post(final String url, final JSONObject payload, final VolleyCallback callback) {
        request(Request.Method.POST, url, payload, callback, this);
    }

    protected void request(final int method, final String url, final JSONObject payload, final VolleyCallback callback, CacheHeaderInterceptor interceptor) {
        HeaderInterceptorJsonObjectRequest jsonRequest = new HeaderInterceptorJsonObjectRequest(method, url, payload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "request onResponse onSuccess " + url);
                        Cache.Entry e = mRequestQueue.getCache().get(url);
                        if (e != null && e.isExpired()) {
                            Log.d(TAG, "request onResponse expired");
                            mRequestQueue.getCache().remove(url);
                        }
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "request onErrorResponse onError " + url);
                        callback.onError(error);

                    }
                }, interceptor
        );

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        mRequestQueue.add(jsonRequest);
    }

    public Cache.Entry interceptCacheHeader(NetworkResponse response) {
        return mInterceptor.interceptCacheHeader(response);
    }
}
