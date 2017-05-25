package com.dfbarone.forgettablerequestcache;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.dfbarone.forgettablerequestcache.request.CacheHeaderInterceptor;
import com.dfbarone.forgettablerequestcache.request.HeaderInterceptorJsonObjectRequest;
import com.dfbarone.forgettablerequestcache.request.ForgettableRequestCacheInterceptor;
import com.dfbarone.forgettablerequestcache.request.HeaderInterceptorRequest;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

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

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    // Get method
    public void requestJSON(final int method,
                            final String url,
                            final JSONObject payload,
                            final Request.Priority priority,
                            final VolleyCallback callback) {

        requestJSON(method, url, payload, priority, callback, this);
    }

    protected void requestJSON(final int method,
                               final String url,
                               final JSONObject payload,
                               final Request.Priority priority,
                               final VolleyCallback callback,
                               CacheHeaderInterceptor interceptor) {

        HeaderInterceptorJsonObjectRequest jsonRequest = new HeaderInterceptorJsonObjectRequest(method, url, payload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "request onResponse onSuccess " + url);
                        Cache.Entry e = mRequestQueue.getCache().get(url);
                        if (e != null && e.isExpired()) {
                            Log.d(TAG, "request onResponse expired");
                            //mRequestQueue.getCache().remove(url);
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
        ) {
            @Override
            public Priority getPriority() {
                return priority;
            }
        };

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/10,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        mRequestQueue.add(jsonRequest);
    }

    // Get method
    public <T> void requestMoshi(final int method,
                                 final String url,
                                 final Class<T> clazz,
                                 final String payload,
                                 final Request.Priority priority,
                                 final int timeout,
                                 final VolleyCallback callback) {

        requestMoshi(method, url, clazz, payload, priority, timeout, callback, this);
    }

    protected <T> void requestMoshi(final int method,
                                    final String url,
                                    final Class<T> clazz,
                                    final String payload,
                                    final Request.Priority priority,
                                    int timeout,
                                    final VolleyCallback<T> callback,
                                    CacheHeaderInterceptor interceptor) {

        /*T t = tryLoadCachedStream(url, clazz);
        if (t != null) {
            Log.d(TAG, "request cached onSuccess " + url);
            callback.onSuccess(t);
        }*/

        final HeaderInterceptorRequest<T> jsonRequest = new HeaderInterceptorRequest<T>(method, url, clazz, payload,
                new Response.Listener<T>() {
                    @Override
                    public void onResponse(T response) {
                        Log.d(TAG, "request onResponse onSuccess " + url);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, onErrorMessage(error) + " " + url);
                        callback.onError(error);

                    }
                }, interceptor){

            @Override
            public Priority getPriority() {
                return priority;
            }
        };

        /*if (timeout > 0) { }*/
            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                    10*1000,
                    10,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        mRequestQueue.add(jsonRequest);
    }

    public String onErrorMessage(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return "Communication Error!";
        } else if (error instanceof AuthFailureError) {
            return "Authentication Error!";
        } else if (error instanceof ServerError) {
            return "Server Side Error!";
        } else if (error instanceof NetworkError) {
            return "Network Error!";
        } else if (error instanceof ParseError) {
            return "Parse Error!";
        }
        return "Unknown Error!";
    }

    public Cache.Entry interceptCacheHeader(NetworkResponse response) {
        return mInterceptor.interceptCacheHeader(response);
    }

    public static String cacheEntryToString(Cache.Entry entry) {
        String jsonString = null;
        try {
            jsonString = new String(entry.data, HttpHeaderParser.parseCharset(entry.responseHeaders, HeaderInterceptorRequest.getCharSet()));
        } catch (UnsupportedEncodingException e) {

        }
        return jsonString;
    }

    private <T> T tryLoadCachedStream(String url, Class<T> clazz) {

        // Try to load the last request for this stream from http cache
        Cache.Entry e = mRequestQueue.getCache().get(url);
        if (e != null && e.data != null) {
            String payload = ForgettableRequestCacheHelper.cacheEntryToString(e);
            if (payload != null) {
                T s = MoshiUtils.parseJSONObject(payload, clazz);
                return s;
            }
        }

        return null;
    }
}

/*final HeaderInterceptorRequest<T> jsonRequest2 = new HeaderInterceptorRequest<>(method, url, clazz, payload,
        new Response.Listener<T>() {
@Override
public void onResponse(T response) {
        Log.d(TAG, "request onResponse onSuccess " + url);
        callback.onSuccess(response);
        }
        },
        new Response.ErrorListener() {
@Override
public void onErrorResponse(VolleyError error) {
        Log.d(TAG, onErrorMessage(error) + " " + url);
        callback.onError(error);


        }
        }, ForgettableRequestCacheHelper.this
        );

        //if (timeout > 0) {
        jsonRequest2.setRetryPolicy(new DefaultRetryPolicy(
        10*1000,
        1,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //}

        mRequestQueue.add(jsonRequest2);*/
