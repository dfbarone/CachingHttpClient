package com.dfbarone.lazyrequestcache.okhttp;

import android.content.Context;
import android.util.Log;

import com.dfbarone.lazyrequestcache.json.JsonConverter;

import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 6/19/17.
 */

public class CachingHttpClient {

    protected static OkHttpClient mHttpClient;
    private static CachingHttpClient mInstance;
    private final String TAG = CachingHttpClient.class.getName();

    public static CachingHttpClient getInstance() {
        if (mInstance == null) {
            mInstance = new CachingHttpClient();
        }
        return mInstance;
    }

    public static void init(Context context) {
        if (mHttpClient == null) {
            mHttpClient = OkHttpClientFactory.okHttpClient(context);
        }
    }

    /**
     * Dangerous interceptor that rewrites the server's cache-control header.
     */
    private static Interceptor cacheControlInterceptor(final long maxAgeSeconds, final long maxStaleSeconds) {
        return new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAgeSeconds + ", max-stale=" + maxStaleSeconds)
                        .build();
            }
        };
    }

    private static String responseToString(Response response) {
        String payload = null;
        try {
            if (response != null) {
                payload = new String(response.body().bytes(), "UTF-8");
                response.close();
            }
        } catch (Exception e) {
        }
        return payload;
    }

    public Response newCall(Request cachingRequest) {

        OkHttpClient client = mHttpClient.newBuilder()
                .addNetworkInterceptor(
                        cacheControlInterceptor(cachingRequest.cacheControl().maxAgeSeconds(),
                                cachingRequest.cacheControl().maxStaleSeconds())
                )
                .build();

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(cachingRequest.url());
        requestBuilder.headers(cachingRequest.headers());
        requestBuilder.method(cachingRequest.method(), cachingRequest.body());

        if (cachingRequest.cacheControl().onlyIfCached()) {
            // Only look in cache for response
            requestBuilder.cacheControl(CacheControl.FORCE_CACHE);
        }

        Request request = requestBuilder.build();

        Call call = client.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            if (response.cacheResponse() != null && response.networkResponse() == null) {
                Log.d(TAG, "get  cached " + cachingRequest.url());
            } else if (response.networkResponse() != null) {
                Log.d(TAG, "get network " + cachingRequest.url());
            } else {
                Log.d(TAG, "get     bad " + cachingRequest.url());
            }
            //response.close();
        } catch (IOException e) {
            Log.d(TAG, "get error " + e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "get error " + e.getMessage());
        }
        return response;
    }

    public String newCallString(Request cachingRequest) {

        String payload = null;
        try {
            Response response = newCall(cachingRequest);
            payload = responseToString(response);
            response.close();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "getString error " + e.getMessage());
        }
        return payload;
    }

    /*public <T> T get(Class<T> clazz, final Map<String, String> headers, final String url) {
        String payload = getString(headers, url);
        if (payload != null) {
            //T s = JsonConverter.gsonFromJson(payload, clazz);
            //return s;
        }
        return null;
    }*/

    public boolean isExpired(Request cachingRequest, long timeToLive) {

        boolean expired = true;
        try {
            Response response = newCall(cachingRequest);
            if (response.networkResponse() == null && response.cacheResponse() != null) {
                Long diff = (System.currentTimeMillis() - response.sentRequestAtMillis()) / 1000;
                if (diff < timeToLive) {
                    expired = false;
                }
                Log.d(TAG, "isExpired " + expired + " " + diff + "s elapsed " + cachingRequest.url());
            } else {
                Log.d(TAG, "isExpired request not cached" + cachingRequest.url());
            }
            response.close();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "isExpired error " + e.getMessage());
        }
        return expired;
    }

    public <T> Observable<T> requestMoshi(final Request cachingRequest, final Class<T> clazz) {

        return Observable.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                Response ret = newCall(cachingRequest);

                T s = null;
                try {
                    String payload = new String(ret.body().bytes(), "UTF-8");
                    if (payload != null) {
                        Log.d(TAG, payload);
                        s = JsonConverter.moshiFromJson(payload, clazz);
                        if (s != null) {
                            return s;
                        } else {
                            throw new IOException("badness");
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "wtf " + e.getStackTrace());
                    throw e;
                }
                return null;
            }
        });
    }
}
