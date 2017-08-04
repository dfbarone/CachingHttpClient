package com.dfbarone.lazyrequestcache.okhttp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dfbarone.lazyrequestcache.IHttpClient;
import com.dfbarone.lazyrequestcache.json.MoshiUtils;
import com.dfbarone.lazyrequestcache.volley.VolleyCallback;

import java.io.IOException;
import java.util.Map;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 6/19/17.
 */

public class LazyOkHttpRequestCacheHelper implements IHttpClient, Interceptor {

    /**
     * Dangerous interceptor that rewrites the server's cache-control header.
     */
    private static final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, max-age=" + 60 + ", max-stale=" + 60*60*24 )
                    .build();
        }
    };
    protected static OkHttpClient mHttpClient;
    private static LazyOkHttpRequestCacheHelper mInstance;
    private final String TAG = LazyOkHttpRequestCacheHelper.class.getName();

    public static LazyOkHttpRequestCacheHelper getInstance() {
        if (mInstance == null) {
            mInstance = new LazyOkHttpRequestCacheHelper();
        }
        return mInstance;
    }

    public static void init(Context context) {
        if (mHttpClient == null) {
            mHttpClient = LazyOkHttp.newRequestQueue(context);
        }
    }

    private static String cacheEntryToString(Response response) {
        try {
            if (response != null) {
                return response.body().string();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public <T> void requestMoshi(final int method,
                                 final Map<String, String> header,
                                 final String url,
                                 final Class<T> clazz,
                                 final String payload,
                                 final com.android.volley.Request.Priority priority,
                                 final int timeout,
                                 final VolleyCallback callback) {

        requestMoshi(method, header, url, clazz, payload, priority, timeout, callback, this);
    }

    protected <T> void requestMoshi(final int method,
                                    final Map<String, String> header,
                                    final String url,
                                    final Class<T> clazz,
                                    final String payload,
                                    final com.android.volley.Request.Priority priority,
                                    int timeout,
                                    final VolleyCallback<T> callback,
                                    Interceptor interceptor) {

        try {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            for (String key : header.keySet()) {
                requestBuilder.addHeader(key, header.get(key));
            }
            Request request = requestBuilder.build();

            OkHttpClient client = mHttpClient.newBuilder()
                .addNetworkInterceptor(interceptor)
                .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(null, null);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String payload = response.body().string();
                        if (payload != null) {
                            Log.d(TAG, payload);
                            final T s = MoshiUtils.parseJSONObject(payload, clazz);
                            if (s != null) {
                                Handler mHandler = new Handler(Looper.getMainLooper());
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess(s);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "wtf" + e.getMessage());
                    }

                }
            });

        } catch (Exception e) {
            Log.d(TAG, "wtf" + e.getMessage());
        }
    }

    public Response intercept(Chain chain) throws IOException {
        return REWRITE_CACHE_CONTROL_INTERCEPTOR.intercept(chain);
    }

    private Response loadCachedResponse(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .cacheControl(new CacheControl.Builder().onlyIfCached().build())
                    .build();

            Response response = mHttpClient.newCall(request).execute();
            response.close();
            if (response.isSuccessful()) {
                return response.cacheResponse();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public String loadCachedResponseToString(String url) {
        Response response = loadCachedResponse(url);
        return cacheEntryToString(response);
    }

    public <T> T loadCachedResponseToMoshi(String url, Class<T> clazz) {
        String payload = loadCachedResponseToString(url);
        if (payload != null) {
            T s = MoshiUtils.parseJSONObject(payload, clazz);
            return s;
        }
        return null;
    }
}
