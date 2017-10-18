package com.dfbarone.cachinghttpclient.okhttp;

import android.content.Context;
import android.util.Log;

import com.dfbarone.cachinghttpclient.okhttp.interceptors.CachingOfflineInterceptor;
import com.dfbarone.cachinghttpclient.okhttp.interceptors.CachingNetworkInterceptor;
import com.dfbarone.cachinghttpclient.okhttp.utils.ConverterUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 6/19/17.
 */

public class CachingOkHttpClient {

    private OkHttpClient okHttpClient;
    private static final String TAG = CachingOkHttpClient.class.getSimpleName();
    private int maxAgeSeconds;

    public CachingOkHttpClient(Context context) {
        Builder builder = new Builder(context);
        okHttpClient = builder.okHttpClientBuilder.build();
        maxAgeSeconds = builder.maxAgeSeconds;
    }

    private CachingOkHttpClient(Builder builder) {
        okHttpClient = builder.okHttpClientBuilder.build();
        maxAgeSeconds = builder.maxAgeSeconds;
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param request standard okhttp3 request for GET call
     * @return Response
     */
    public Response getResponse(Request request) throws IOException {
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        if (response.networkResponse() != null && response.cacheResponse() != null) {
            Log.d(TAG, "get cond'tnl " + response.networkResponse().code() + " " + request.url());
        } else if (response.networkResponse() != null) {
            Log.d(TAG, "get  network " + response.networkResponse().code() + " " + request.url());
        } else if (response.cacheResponse() != null) {
            Log.d(TAG, "get   cached " + response.cacheResponse().code() + " " + request.url());
        } else {
            Log.d(TAG, "get      bad " + response.code() + " " + request.url());
        }
        return response;
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param request standard okhttp3 request for GET call
     * @return Response
     */
    public Single<Response> getResponseAsync(final Request request) throws IOException {
        return Single.fromCallable(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return getResponse(request);
            }
        });
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param request standard okhttp3 request for GET call
     * @return String response body
     */
    public String getString(Request request) throws IOException {
        String payload = null;
        try {
            Response response = getResponse(request);
            payload = ConverterUtils.responseToString(response);
            response.close();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "getString error " + e.getMessage());
        }
        return payload;
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param request standard okhttp3 request for GET call
     * @return String response body
     */
    public Single<String> getStringAsync(final Request request) {
        return Single.fromCallable(new Callable<String>() {
            @Override
            public String call() throws IOException {
                return getString(request);
            }
        });
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param request standard okhttp3 request for GET call
     * @return Moshi deserialized class of response body
     */
    public <T extends Object> T get(final Request request, final Class<T> clazz) throws IOException {
        Response response = getResponse(request);
        T payloadT = null;
        try {
            String payload = new String(response.body().bytes(), "UTF-8");
            if (payload != null) {
                payloadT = ConverterUtils.moshiFromJson(payload, clazz);
                if (payloadT != null) {
                    return payloadT;
                } else {
                    throw new IOException("parse exception");
                }
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "get error " + e.getMessage());
        }
        return payloadT;
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param request standard okhttp3 request for GET call
     * @return Moshi deserialized class of response body
     */
    public <T extends Object> Single<T> getAsync(final Request request, final Class<T> clazz) {
        return Single.fromCallable(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return get(request, clazz);
            }
        });
    }

    /**
     * A helper method to determine if your http GET is expired.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to maxAgeSeconds
     *
     * @param request standard okhttp3 request for GET call
     * @return true of exipired in disk cache
     */
    public boolean isExpired(Request request) {
        try {
            int maxAge = maxAgeSeconds;
            if (request.cacheControl() != null && request.cacheControl().maxAgeSeconds() > -1) {
                maxAge = request.cacheControl().maxAgeSeconds();
            }

            // Checking if a response is expired requires getting from cache only
            Response response = getResponse(request.newBuilder()
                    .cacheControl(new CacheControl.Builder()
                            .onlyIfCached()
                            .maxAge(maxAge, TimeUnit.SECONDS)
                            .build())
                    .build()
            );

            if (response != null && response.cacheResponse() != null && response.isSuccessful()) {
                long diff = (System.currentTimeMillis() - response.receivedResponseAtMillis()) / 1000;
                Log.d(TAG, "isExpired false " + diff + "s elapsed " + request.url());
                return false;
            } else {
                Log.d(TAG, "isExpired true " + request.url());
            }
            response.close();
        } catch (Exception e) {
            Log.d(TAG, "isExpired error " + e.getMessage());
        }
        return true;
    }

    public static final class Builder {

        public static final int MAX_AGE_SECONDS = 60;
        private static final int DEFAULT_DISK_SIZE_BYTES = 10 * 1024 * 1024;
        private static final String DEFAULT_CACHE_DIR = "caching_ok_http_client";

        private OkHttpClient.Builder okHttpClientBuilder;
        private Context context;
        private Cache cache;
        private int maxAgeSeconds;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
            this.okHttpClientBuilder = null;
            this.cache = null;
            this.maxAgeSeconds = MAX_AGE_SECONDS;
        }

        public Builder cache(Cache cache) {
            this.cache = cache;
            return this;
        }

        public Builder cache(String cacheDirectory, int diskSizeInBytes) {
            this.cache = getCache(context, cacheDirectory, diskSizeInBytes);;
            return this;
        }

        public Builder cache() {
            this.cache = getCache(context, DEFAULT_CACHE_DIR, DEFAULT_DISK_SIZE_BYTES);
            return this;
        }

        public Builder maxAge(int maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
            return this;
        }

        public Builder okHttpClient(OkHttpClient okHttpClient) {
            this.okHttpClientBuilder = okHttpClient.newBuilder();
            return this;
        }

        public Builder okHttpClient(OkHttpClient.Builder okHttpClientBuilder) {
            this.okHttpClientBuilder = okHttpClientBuilder;
            return this;
        }

        public CachingOkHttpClient build() {

            // If no default ok http client, make one.
            if (okHttpClientBuilder == null) {
                okHttpClientBuilder = new OkHttpClient.Builder();
            }

            // If cache has been set, override.
            if (cache != null) {
                okHttpClientBuilder.cache(cache);
            }

            // Add interceptors to enforce
            // A) max-age when GET responses are cached
            // B) max-stale when GET requests are made offline
            okHttpClientBuilder
                    .addNetworkInterceptor(new CachingNetworkInterceptor(maxAgeSeconds))
                    .addInterceptor(new CachingOfflineInterceptor(context));

            // Retry can't hurt? right?
            okHttpClientBuilder.retryOnConnectionFailure(true);

            // This is a major kludge to fix a bug in okhttp3
            // Switching networks will cause old socket connections to not
            // get killed. The workaround is to set the connection pool below.
            // https://github.com/square/okhttp/issues/3146
            okHttpClientBuilder.connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS));

            return new CachingOkHttpClient(this);
        }

        /*
         * Utility methods
         */
        public static Cache getCache(Context context, String cacheDirName, int diskCacheSizeInBytes) {
            File cacheDir = new File(getCacheDir(context), cacheDirName);
            cacheDir.mkdirs();
            return new Cache(cacheDir, diskCacheSizeInBytes);
        }

        private static File getCacheDir(Context context) {
            File rootCache = context.getExternalCacheDir();
            if (rootCache == null) {
                rootCache = context.getCacheDir();
            }
            return rootCache;
        }
    }

}
