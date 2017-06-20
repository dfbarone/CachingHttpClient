package com.dfbarone.lazyrequestcache.volley;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
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
import com.android.volley.toolbox.RequestFuture;
import com.dfbarone.lazyrequestcache.json.MoshiUtils;
import com.dfbarone.lazyrequestcache.volley.request.CacheHeaderInterceptor;
import com.dfbarone.lazyrequestcache.volley.request.HeaderInterceptorRequest;
import com.dfbarone.lazyrequestcache.volley.request.HeaderInterceptorStringRequest;
import com.dfbarone.lazyrequestcache.volley.request.LazyRequestCacheInterceptor;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Single;

/**
 * Created by dbarone on 5/12/2017.
 */

public class LazyRequestCacheHelper implements CacheHeaderInterceptor {

    private final String TAG = LazyRequestCacheHelper.class.getSimpleName();


    protected static RequestQueue mHttpClient;

    protected LazyRequestCacheInterceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new LazyRequestCacheInterceptor();

    private static LazyRequestCacheHelper mInstance;

    public static LazyRequestCacheHelper getInstance() {
        if (mInstance == null) {
            mInstance = new LazyRequestCacheHelper();
        }
        return mInstance;
    }

    public static void init(Context context) {
        if (mHttpClient == null) {
            mHttpClient = LazyVolley.newRequestQueue(context);
        }
    }

    public RequestQueue getRequestQueue() {
        return mHttpClient;
    }

    // Get method
    public Single<String> requestString(final int method,
                                        final String url,
                                        final Request.Priority priority) {

        return requestString(method, url, null, priority, 0, this);
    }

    protected Single<String> requestString(final int method,
                                           final String url,
                                           final String payload,
                                           final Request.Priority priority,
                                           int timeout,
                                           CacheHeaderInterceptor interceptor) {

        RequestFuture<String> requestFuture = RequestFuture.newFuture();

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                //Log.d(TAG, "requestString - success. " + url);
                return;
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                final String payload = loadCachedResponseToString(url);
                if (payload != null) {
                    Log.d(TAG, "requestString - error. return cached response " + url);
                    //callback.onSuccess(payload);
                } else {
                    Log.d(TAG, "requestString - error. " + onErrorMessage(error) + " " + url);
                }

                //callback.onError(error, payload);
            }
        };


        final HeaderInterceptorStringRequest jsonRequest =
                new HeaderInterceptorStringRequest(method, url, requestFuture, requestFuture, REWRITE_CACHE_CONTROL_INTERCEPTOR) {

                    @Override
                    public Priority getPriority() {
                        return priority;
                    }
                };

        /*if (timeout > 0) { }
            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                    10*1000,
                    10,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));*/

        mHttpClient.add(jsonRequest);

        try {
            String response = null;
            while (response == null) {
                try {
                    response = requestFuture.get(30, TimeUnit.SECONDS); // Block thread, waiting for response, timeout after 30 seconds
                } catch (InterruptedException e) {
                    // Received interrupt signal, but still don't have response
                    // Restore thread's interrupted status to use higher up on the call stack
                    Thread.currentThread().interrupt();
                    // Continue waiting for response (unless you specifically intend to use the interrupt to cancel your request)
                }
            }
            // Do something with response, i.e.

        } catch (TimeoutException e) {

        } catch (ExecutionException e) {
            // Do something with error, i.e.

        }

        return Single.fromFuture(requestFuture, 30, TimeUnit.SECONDS);
    }

    /*public void requestJSON(final int method,
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
                               final VolleyCallback<JSONObject> callback,
                               CacheHeaderInterceptor interceptor) {

        HeaderInterceptorJsonObjectRequest jsonRequest = new HeaderInterceptorJsonObjectRequest(method, url, payload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "request onResponse onSuccess " + url);
                        Cache.Entry e = mHttpClient.getCache().get(url);
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
                        callback.onError(error, null);

                    }
                }, interceptor
        ) {
            @Override
            public Priority getPriority() {
                return priority;
            }
        };

        //jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
        //        DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
        //        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        //        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        mHttpClient.add(jsonRequest);
    }*/

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

        final HeaderInterceptorRequest<T> jsonRequest = new HeaderInterceptorRequest<T>(method, url, clazz, payload,
                new Response.Listener<T>() {
                    @Override
                    public void onResponse(T response) {
                        //Log.d(TAG, "requestMoshi - success. " + url);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        final T payload = loadCachedResponseToMoshi(url, clazz);
                        if (payload != null) {
                            Log.d(TAG, "requestMoshi - error. return cached response " + url);
                            callback.onSuccess(payload);
                        } else {
                            Log.d(TAG, "requestMoshi - error. " + onErrorMessage(error) + " " + url);
                        }

                        callback.onError(error, payload);

                    }
                }, interceptor) {

            @Override
            public Priority getPriority() {
                return priority;
            }
        };

        /*if (timeout > 0) { }
            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                    10*1000,
                    10,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));*/

        mHttpClient.add(jsonRequest);
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
        return REWRITE_CACHE_CONTROL_INTERCEPTOR.interceptCacheHeader(response);
    }

    private static String cacheEntryToString(Cache.Entry entry) {
        String jsonString = null;
        try {
            jsonString = new String(entry.data, HttpHeaderParser.parseCharset(entry.responseHeaders, HeaderInterceptorRequest.getCharSet()));
        } catch (UnsupportedEncodingException e) {

        }
        return jsonString;
    }

    private Cache.Entry loadCachedResponse(String url) {
        // Try to load the last request for this stream from http cache
        Cache.Entry e = mHttpClient.getCache().get(url);
        if (e != null && e.data != null) {
            return e;
        }
        return null;
    }

    public String loadCachedResponseToString(String url) {
        // Try to load the last request for this stream from http cache
        Cache.Entry e = loadCachedResponse(url);
        if (e != null && e.data != null) {
            String payload = LazyRequestCacheHelper.cacheEntryToString(e);
            if (payload != null) {
                return payload;
            }
        }
        return null;
    }

    public <T> T loadCachedResponseToMoshi(String url, Class<T> clazz) {
        // Try to load the last request for this stream from http cache
        String payload = loadCachedResponseToString(url);
        if (payload != null) {
            T s = MoshiUtils.parseJSONObject(payload, clazz);
            return s;
        }
        return null;
    }
}
