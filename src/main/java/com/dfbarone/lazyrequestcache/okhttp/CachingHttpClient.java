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

    public static final String WARNING_RESPONSE_IS_STALE = "110";
    protected static OkHttpClient mHttpClient;
    private static CachingHttpClient mInstance;
    private static Context mContext;
    public static final String TAG = CachingHttpClient.class.getSimpleName();
    public static final long MAX_AGE = 60;
    public static final long MAX_STALE = 60*60*24*365;

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

    public static boolean isStaleResponse(Response response) {
        // not much we can do in this case
        if (response == null) {
            return false;
        }

        List<String> warningHeaders = response.headers("Warning");

        for (String warningHeader : warningHeaders) {
            // if we can find a warning header saying that this response is stale, we know
            // that we can't skip it.
            if (warningHeader.startsWith(WARNING_RESPONSE_IS_STALE)) {
                return true;
            }
        }

        return false;
    }

    public static Request removeCacheHeaders(Request request) {
        Headers modifiedHeaders = request.headers()
                .newBuilder()
                .removeAll("Cache-Control")
                .build();

        return request.newBuilder()
                .headers(modifiedHeaders)
                .build();
    }

    public static Response rewriteCacheControlIfConnected(Context context, Response originalResponse) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return setMaxStale(originalResponse);
        } else {
            return setMaxAge(originalResponse);
        }
    }

    public static Response setMaxStale(Response originalResponse) {
        return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-stale=" + MAX_STALE)
                .build();
    }

    public static Response setMaxAge(Response originalResponse) {
        return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + MAX_AGE)
                .build();
    }

    /**
     * Interceptor to cache data and maintain it for a minute.
     *
     * If the same network request is sent within a minute,
     * the response is retrieved from cache.
     */
    private static class DefaultInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Log.d(CachingHttpClient.TAG, "DefaultInterceptor " + chain.request().url());
            Request request = chain.request();
            Response originalResponse = chain.proceed(request);
            if (!isStaleResponse(originalResponse)) {
                return rewriteCacheControlIfConnected(mContext, originalResponse);
            } else {
                Request modifiedRequest = removeCacheHeaders(request);
                try {
                    Response retriedResponse = chain.proceed(modifiedRequest);
                    if (retriedResponse == null || !retriedResponse.isSuccessful()) {
                        return rewriteCacheControlIfConnected(mContext, originalResponse);
                    }
                    return rewriteCacheControlIfConnected(mContext, retriedResponse);
                } catch (IOException e) {
                    return rewriteCacheControlIfConnected(mContext, originalResponse);
                }
            }
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
            Request request = chain.request();
            Response originalResponse = chain.proceed(request);
            if (!isStaleResponse(originalResponse)) {
                return rewriteCacheControlIfConnected(mContext, originalResponse);
            } else {
                Request modifiedRequest = removeCacheHeaders(request);
                try {
                    Response retriedResponse = chain.proceed(modifiedRequest);
                    if (retriedResponse == null || !retriedResponse.isSuccessful()) {
                        return rewriteCacheControlIfConnected(mContext, originalResponse);
                    }
                    return rewriteCacheControlIfConnected(mContext, retriedResponse);
                } catch (IOException e) {
                    return rewriteCacheControlIfConnected(mContext, originalResponse);
                }
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
        requestBuilder.method(cachingRequest.method(), cachingRequest.body());

        if (cachingRequest.cacheControl().onlyIfCached()) {
            // Only look in cache for response
            requestBuilder.cacheControl(CacheControl.FORCE_NETWORK);
        } else {
            clientBuilder.addNetworkInterceptor(new NetworkInterceptor())
                    .addInterceptor(new DefaultInterceptor());
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
