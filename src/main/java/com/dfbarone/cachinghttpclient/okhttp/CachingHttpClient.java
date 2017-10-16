package com.dfbarone.cachinghttpclient.okhttp;

import android.content.Context;
import android.util.Log;

import com.dfbarone.cachinghttpclient.json.JsonConverter;
import com.dfbarone.cachinghttpclient.utils.NetworkUtils;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 6/19/17.
 */

public class CachingHttpClient {

    private OkHttpClient mHttpClient;
    private static Context mContext;
    private static final String TAG = CachingHttpClient.class.getSimpleName();
    private static final int MAX_AGE = 60;
    private static final int MAX_STALE = 60 * 60 * 24 * 365;

    public CachingHttpClient(Context context) {
        mContext = context.getApplicationContext();
        mHttpClient = OkHttpClientFactory.okHttpClient(context);
    }

    public CachingHttpClient(Context context, OkHttpClient okHttpClient) {
        mContext = context.getApplicationContext();
        mHttpClient = okHttpClient;
    }

    /**
     * Interceptor to cache data and maintain it for a minute.
     * <p>
     * If the same network request is sent within a minute,
     * the response is retrieved from cache.
     */
    private static class OfflineResponseCacheInterceptor implements Interceptor {

        private int mMaxStale;

        public OfflineResponseCacheInterceptor(int maxStale) {
            mMaxStale = maxStale;
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            // If offline, request based on max-stale
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                request = request.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + mMaxStale)
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

        private int mMaxAge;

        public ResponseCacheNetworkInterceptor(int maxAge) {
            mMaxAge = maxAge;
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, max-age=" + mMaxAge)
                    .build();
        }
    }

    private Call newCall(Request request) {
        OkHttpClient.Builder clientBuilder = mHttpClient.newBuilder();
        // When FORCE_CACHE is not set
        if (!request.cacheControl().onlyIfCached()) {
            // Add interceptors
            clientBuilder
                    .addNetworkInterceptor(new ResponseCacheNetworkInterceptor(request.cacheControl().maxAgeSeconds()))
                    .addInterceptor(new OfflineResponseCacheInterceptor(MAX_STALE));

            // When making GET calls...
            if (request.method().equalsIgnoreCase("get")) {
                // Customizing a connection pool is a major kludge. Switching networks
                // will cause old socket connections to not get killed. The workaround is to
                // set the connection pool below.
                // https://github.com/square/okhttp/issues/3146
                clientBuilder.retryOnConnectionFailure(true)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS));
            }
        }
        return clientBuilder.build().newCall(request);
    }

    public Response getResponse(Request request) throws IOException {
        try {
            Call call = newCall(request);
            Response response = call.execute();
            if (response.networkResponse() != null && response.cacheResponse() != null) {
                Log.d(TAG, "get cond'tnl " + response.code() + " " + request.url());
            } else if (response.networkResponse() != null) {
                Log.d(TAG, "get  network " + response.code() + " " + request.url());
            } else if (response.cacheResponse() != null) {
                Log.d(TAG, "get   cached " + response.code() + " " + request.url());
            } else {
                Log.d(TAG, "get      bad " + response.code() + " " + request.url());
            }
            //response.close();
            return response;
        } catch (IOException e) {
            Log.d(TAG, "getResponse error " + e.getMessage() + e.getStackTrace());
            throw e;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "getResponse error " + e.getMessage() + e.getStackTrace());
        }
        return null;
    }

    public String getString(Request request) throws IOException {
        String payload = null;
        try {
            Response response = getResponse(request);
            payload = OkHttpUtils.responseToString(response);
            response.close();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "getString error " + e.getMessage());
        }
        return payload;
    }

    public Single<String> getStringAsync(final Request request) {
        return Single.fromCallable(new Callable<String>() {
            @Override
            public String call() throws IOException {
                return getString(request);
            }
        });
    }

    public <T> T get(final Request request, final Class<T> clazz) throws IOException {
        Response response = getResponse(request);
        T payloadT = null;
        try {
            String payload = new String(response.body().bytes(), "UTF-8");
            if (payload != null) {
                payloadT = JsonConverter.moshiFromJson(payload, clazz);
                if (payloadT != null) {
                    return payloadT;
                } else {
                    throw new IOException("badness");
                }
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "get error " + e.getMessage());
        }
        return payloadT;
    }

    public <T> Single<T> getAsync(final Request request, final Class<T> clazz) {
        return Single.fromCallable(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return get(request, clazz);
            }
        });
    }

    public boolean isExpired(Request request) {
        boolean expired = true;
        try {
            // Checking if a response is expired requires getting from cache only
            Response response = getResponse(request.newBuilder()
                    .cacheControl(new CacheControl.Builder()
                            .onlyIfCached()
                            .maxAge(request.cacheControl().maxAgeSeconds(), TimeUnit.SECONDS)
                            .build())
                    .build()
            );

            if (response != null && response.isSuccessful()) {
                long diff = (System.currentTimeMillis() - response.receivedResponseAtMillis()) / 1000;
                expired = false;
                Log.d(TAG, "isExpired false " + diff + "s elapsed " + request.url());
            } else {
                Log.d(TAG, "isExpired true " + request.url());
            }
            response.close();
        } catch (IOException e) {
            Log.d(TAG, "isExpired error " + e.getMessage());
        }
        return expired;
    }

}
