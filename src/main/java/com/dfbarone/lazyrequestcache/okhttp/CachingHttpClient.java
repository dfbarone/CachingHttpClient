package com.dfbarone.lazyrequestcache.okhttp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.dfbarone.lazyrequestcache.json.JsonConverter;
import com.dfbarone.lazyrequestcache.utils.NetworkUtils;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
    private static Context mContext;
    public static final String TAG = CachingHttpClient.class.getSimpleName();
    public static final long MAX_AGE = 30;
    public static final long MAX_STALE = 60*60;

    public static CachingHttpClient getInstance() {
        if (mInstance == null) {
            mInstance = new CachingHttpClient();
        }
        return mInstance;
    }

    public static void init(Context context) {
        if (mHttpClient == null) {
            mContext = context.getApplicationContext();
            mHttpClient = OkHttpClientFactory.okHttpClient(context, 16*1000*1000);
        }
    }

    /**
     * Dangerous interceptor that rewrites the server's cache-control header.
     */
    /*private static Interceptor cacheControlInterceptor(final long maxAgeSeconds, final long maxStaleSeconds) {
        return new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAgeSeconds + ", max-stale=" + maxStaleSeconds)
                        .build();
            }
        };
    }*/

    /**
     * Interceptor to cache data and maintain it for a minute.
     *
     * If the same network request is sent within a minute,
     * the response is retrieved from cache.
     */
    private static class DefaultInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Log.d(CachingHttpClient.TAG, "DefaultInterceptor " + chain.request().header("Cache-Control") + chain.request().url());
            Request.Builder builder = chain.request().newBuilder();
            return chain.proceed(builder.build());
        }
    }

    /**
     * Interceptor to cache data and maintain it for four weeks.
     *
     * If the device is offline, stale (at most four weeks old)
     * response is fetched from the cache.
     */
    private static class NetworkInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Log.d(CachingHttpClient.TAG, "NetworkInterceptor " + chain.request().url());
            //Request.Builder builder = chain.request().newBuilder();
            okhttp3.Response originalResponse = chain.proceed(chain.request());
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + MAX_STALE)
                        .build();
            } else {
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + MAX_AGE)
                        .build();
            }
        }
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

        OkHttpClient.Builder clientBuilder = mHttpClient.newBuilder();

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(cachingRequest.url());
        requestBuilder.headers(cachingRequest.headers());
        //requestBuilder.header("Cache-Control", "public, max-age=" + MAX_AGE + ", max-stale=" + MAX_STALE);
        requestBuilder.method(cachingRequest.method(), cachingRequest.body());

        clientBuilder.addNetworkInterceptor(new NetworkInterceptor());
                //.addInterceptor(new DefaultInterceptor());

        if (cachingRequest.cacheControl().onlyIfCached()) {
            // Only look in cache for response
            requestBuilder.cacheControl(CacheControl.FORCE_NETWORK);
        }



        Request request = requestBuilder.build();

        Call call = clientBuilder.build().newCall(request);
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
            Log.d(TAG, "get error " + e.getMessage() + e.getStackTrace());
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "get error " + e.getMessage() + e.getStackTrace());
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

    public <T> T newCall(final Request cachingRequest, final Class<T> clazz) throws IOException {
        Response response = newCall(cachingRequest);

        T s = null;
        try {
            String payload = new String(response.body().bytes(), "UTF-8");
            if (payload != null) {
                //Log.d(TAG, payload);
                s = JsonConverter.moshiFromJson(payload, clazz);
                if (s != null) {
                    return s;
                } else {
                    throw new IOException("badness");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "wtf " + e.getStackTrace());
            //throw e;
        }
        return null;
    }

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

}
