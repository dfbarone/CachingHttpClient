package com.dfbarone.lazyrequestcache.okhttp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.dfbarone.lazyrequestcache.json.JsonConverter;
import com.dfbarone.lazyrequestcache.utils.NetworkUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Headers;
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
    public static final int MAX_AGE = 60;
    public static final int MAX_STALE = 60 * 60 * 24 * 365;

    public static CachingHttpClient getInstance() {
        if (mInstance == null) {
            mInstance = new CachingHttpClient();
        }
        return mInstance;
    }

    public static void init(Context context) {
        if (mHttpClient == null) {
            mContext = context.getApplicationContext();
            mHttpClient = OkHttpClientFactory.okHttpClient(context, 16 * 1000 * 1000);
        }
    }

    /**
     * Interceptor to cache data and maintain it for a minute.
     * <p>
     * If the same network request is sent within a minute,
     * the response is retrieved from cache.
     */
    private static class OfflineResponseCacheInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            // Use request set to max-age. This should not be needed,
            // but default request adds max-stale for unknown reasons
            Request request = chain.request()
                    .newBuilder()
                    .header("Cache-Control", "public, max-age=" + MAX_AGE)
                    .build();

            // If offline, request based on max-stale
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                request = request.newBuilder()
                        .header("Cache-Control", "public, max-stale=" + MAX_STALE)
                        .build();
            }

            return chain.proceed(request);
        }
    }

    /**
     * Interceptor to cache data and maintain it MAX_AGE
     * <p>
     * If the same network request is sent within a minute,
     * the response is retrieved from cache.
     */
    private static class ResponseCacheNetworkInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, max-age=" + MAX_AGE)
                    .build();
        }
    }

    public Response newCall(Request cachingRequest) {

        OkHttpClient.Builder clientBuilder = mHttpClient.newBuilder();

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(cachingRequest.url());
        requestBuilder.headers(cachingRequest.headers());
        requestBuilder.method(cachingRequest.method(), cachingRequest.body());

        // Don't add interceptors when looking in cache for responses
        if (cachingRequest.cacheControl().onlyIfCached()) {
            requestBuilder.cacheControl(CacheControl.FORCE_NETWORK);
        } else {
            clientBuilder.addNetworkInterceptor(new ResponseCacheNetworkInterceptor())
                    .addInterceptor(new OfflineResponseCacheInterceptor());
        }

        Request request = requestBuilder.build();

        Call call = clientBuilder.build().newCall(request);
        Response response = null;
        try {
            response = call.execute();
            if (response.cacheResponse() != null && response.networkResponse() == null) {
                Log.d(TAG, "get  cached " + response.cacheResponse().code() + " " + cachingRequest.url());
            } else if (response.networkResponse() != null) {
                Log.d(TAG, "get network " + response.networkResponse().code() + " " + cachingRequest.url());
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
            payload = OkHttpUtils.responseToString(response);
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
            Log.d(TAG, "wtf " + e.getMessage());
            e.printStackTrace();
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
