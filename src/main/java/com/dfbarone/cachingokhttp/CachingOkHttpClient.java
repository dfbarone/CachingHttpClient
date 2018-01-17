package com.dfbarone.cachingokhttp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.Cache;
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

public class CachingOkHttpClient {

    private OkHttpClient okHttpClient;
    private static final String TAG = CachingOkHttpClient.class.getSimpleName();
    private int maxAgeSeconds;
    private int maxStaleSeconds;
    private Context context;
    private CachingInterface dataStore;

    public CachingOkHttpClient(Context context) {
        this.context = context;
        Builder builder = new Builder(context);
        okHttpClient = builder.okHttpClient;
        maxAgeSeconds = builder.maxAgeSeconds;
        maxStaleSeconds = builder.maxStaleSeconds;
        dataStore = builder.dataStore;
    }

    // For calling inside Builder.build() method
    private CachingOkHttpClient(Builder builder) {
        this.context = builder.context;
        this.okHttpClient = builder.okHttpClient;
        this.maxAgeSeconds = builder.maxAgeSeconds;
        this.maxStaleSeconds = builder.maxStaleSeconds;
        this.dataStore = builder.dataStore;
    }

    public OkHttpClient okHttpClient() {
        return okHttpClient;
    }

    public CachingOkHttpClient.Builder newBuilder() {
        return new Builder(this);
    }

    public CachingInterface dataStore() {
        return dataStore;
    }

    /**
     * Custom Per request max age control of cached responses
     *
     * @param cachingRequest
     * @return
     */
    public Call newCall(CachingRequest cachingRequest) {
        OkHttpClient.Builder okHttpClientBuilder = okHttpClient.newBuilder();

        if (cachingRequest.maxAgeSeconds() >= 0) {
            removeInterceptor(okHttpClientBuilder.networkInterceptors(),
                    CacheControlNetworkInterceptor.class);

            okHttpClientBuilder.addNetworkInterceptor(new CacheControlNetworkInterceptor(cachingRequest.maxAgeSeconds()));
        }

        return okHttpClientBuilder.build()
                .newCall(cachingRequest.request());
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param cachingRequest standard okhttp3 request for GET call
     * @return Response
     */
    public Response getResponse(CachingRequest cachingRequest) throws IOException {
        Call call = newCall(cachingRequest);
        Response response = null;
        try {
            response = call.execute();
            logResponse(cachingRequest.request(), response, "get");
            if (dataStore != null) {
                String payload = CachingOkHttpClient.Utilities.responseToString(response.peekBody(Long.MAX_VALUE).bytes());
                if (payload != null) {
                    dataStore.store(response, payload);
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "getString error " + e.getMessage());
            throw e;
        }
        return response;
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param cachingRequest standard okhttp3 request for GET call
     * @return Response
     */
    public Observable<Response> getResponseAsync(final CachingRequest cachingRequest) {
        return Observable.fromCallable(() -> {
            Call call = newCall(cachingRequest);
            Response response = call.execute();
            logResponse(cachingRequest.request(), response, "get");
            return response;
        })
                .doOnNext(response -> {
                    if (dataStore != null) {
                        String payload = CachingOkHttpClient.Utilities.responseToString(response.peekBody(Long.MAX_VALUE).bytes());
                        if (payload != null) {
                            dataStore.store(response, payload);
                        }
                    }
                })
                .doOnError(error -> {
                    Log.d(TAG, "getString error " + error.getMessage());
                });
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param cachingRequest standard okhttp3 request for GET call
     * @return String response body
     */
    public String getString(CachingRequest cachingRequest) throws IOException {
        String payload = null;
        Response response = getResponse(cachingRequest);
        try {
            payload = CachingOkHttpClient.Utilities.responseToString(response.body().bytes());
            response.close();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "getString error " + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "getString error " + e.getMessage());
        }
        return payload;
    }

    /**
     * Caching enabled http GET based on max age.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to 60s
     *
     * @param cachingRequest standard okhttp3 request for GET call
     * @return String response body
     */
    public Single<String> getStringAsync(final CachingRequest cachingRequest) {
        return Single.fromObservable(getResponseAsync(cachingRequest)
                .map(response -> {
                    String payload = null;
                    try {
                        payload = CachingOkHttpClient.Utilities.responseToString(response.body().bytes());
                        response.close();
                    } catch (IllegalArgumentException e) {
                        Log.d(TAG, "getString error " + e.getMessage());
                    } catch (Exception e) {
                        Log.d(TAG, "getString error " + e.getMessage());
                    }
                    return payload;
                }));
    }

    /**
     * A helper method to determine if your http GET is expired.
     * CacheControl.maxAgeSeconds is required to set maxAge of the response
     * if it is not set it will default to maxAgeSeconds
     *
     * @param cachingRequest standard okhttp3 request for GET call
     * @return true of exipired in disk cache
     */
    public boolean isExpired(CachingRequest cachingRequest) {
        try {
            int maxAge = maxAgeSeconds;
            if (cachingRequest.maxAgeSeconds() > -1) {
                maxAge = cachingRequest.maxAgeSeconds();
            }

            // Checking if a response is expired requires getting from cache only
            Request newRequest = cachingRequest.request().newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();

            Call call = okHttpClient.newCall(newRequest);
            Response response = call.execute();
            //logResponse(newRequest, response, "isExpired");

            if (response != null && response.cacheResponse() != null && response.isSuccessful()) {
                long diff = (System.currentTimeMillis() - response.receivedResponseAtMillis()) / 1000;
                response.close();
                Log.d(TAG, "isExpired " + (diff > maxAge) + " " + diff + "s");
                return diff > maxAge;
            }
            response.close();

            if (dataStore != null) {
                Object responseObj = dataStore.load(cachingRequest.request());
                if (responseObj instanceof ResponseEntry) {
                    ResponseEntry pojo = (ResponseEntry) responseObj;
                    if (pojo.timestamp != null) {
                        long diff = (System.currentTimeMillis() - Long.valueOf(pojo.timestamp)) / 1000;
                        response.close();
                        Log.d(TAG, "isExpired " + (diff > maxAge) + " " + diff + "s");
                        return diff > maxAge;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "isExpired error " + e.getMessage());
        }
        Log.d(TAG, "isExpired " + true);
        return false;
    }

    private static void cancel(OkHttpClient client, Object tag) {
        for (Call call : client.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) call.cancel();
        }
        for (Call call : client.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) call.cancel();
        }
    }

    /**
     * Remove all instances of a specific type of interceptor.
     *
     * @param intereceptors a list of interceptors
     * @param clazz the class type of new interceptor
     * @param <T>
     */
    public static <T> void removeInterceptor(List<Interceptor> intereceptors, Class<T> clazz) {
        for (Interceptor i : intereceptors) {
            if (clazz.isInstance(i)) {
                intereceptors.remove(i);
            }
        }
    }

    public static void logResponse(Request request, Response response, String prefix) {
        try {
            if (response.networkResponse() != null && response.cacheResponse() != null) {
                Log.d(TAG, prefix + "  cond'tnl " + response.networkResponse().code() + " " + request.url());
            } else if (response.networkResponse() != null) {
                Log.d(TAG, prefix + "   network " + response.networkResponse().code() + " " + request.url());
            } else if (response.cacheResponse() != null) {
                long diff = (System.currentTimeMillis() - response.receivedResponseAtMillis()) / 1000;
                Log.d(TAG, prefix + "    cached " + response.cacheResponse().code() + " " + diff + "s old" + " " + request.url());
            } else {
                Log.d(TAG, prefix + " not found " + response.code() + " " + response.message() + " " + request.url());
            }
        } catch (Exception e) {

        }
    }

    public static final class Builder {

        public static final int MAX_AGE_SECONDS = 60;
        public static final int MAX_STALE_SECONDS = 60 * 60 * 24 * 356;
        private static final int DEFAULT_DISK_SIZE_BYTES = 10 * 1024 * 1024;
        private static final String DEFAULT_CACHE_DIR = "caching_ok_http_client";

        private OkHttpClient okHttpClient;
        private Context context;
        private Cache cache;
        private CachingInterface dataStore;
        private int maxAgeSeconds;
        private int maxStaleSeconds;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
            this.okHttpClient = null;
            this.cache = null;
            this.dataStore = null;
            this.maxAgeSeconds = MAX_AGE_SECONDS;
            this.maxStaleSeconds = MAX_STALE_SECONDS;
        }

        public Builder(CachingOkHttpClient cachingOkHttpClient) {
            this.context = cachingOkHttpClient.context;
            this.okHttpClient = cachingOkHttpClient.okHttpClient();
            this.cache = null;
            this.dataStore = null;
            this.maxAgeSeconds = cachingOkHttpClient.maxAgeSeconds;
            this.maxStaleSeconds = cachingOkHttpClient.maxStaleSeconds;
        }

        public Builder cache(Cache httpCache) {
            this.cache = httpCache;
            return this;
        }

        public Builder cache(String cacheDirectory, int diskSizeInBytes) {
            this.cache = getCache(context, cacheDirectory, diskSizeInBytes);
            return this;
        }

        public Builder cache() {
            this.cache = getCache(context, DEFAULT_CACHE_DIR, DEFAULT_DISK_SIZE_BYTES);
            return this;
        }

        public Builder dataStore(CachingInterface dataStore) {
            this.dataStore = dataStore;
            return this;
        }

        public Builder maxAge(int maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
            return this;
        }

        public Builder maxStale(int maxStaleSeconds) {
            this.maxStaleSeconds = maxStaleSeconds;
            return this;
        }

        public Builder okHttpClient(OkHttpClient okHttpClient) throws IllegalArgumentException {
            if (okHttpClient == null) {
                throw new IllegalArgumentException("OkHttpClient cannot be null");
            }
            this.okHttpClient = okHttpClient;
            return this;
        }

        public CachingOkHttpClient build() {

            // If no default ok http client, make one.
            if (okHttpClient == null) {
                throw new IllegalArgumentException("OkHttpClient cannot be null");
            }

            OkHttpClient.Builder okHttpClientBuilder = okHttpClient.newBuilder();

            // If cache has been set, override.
            if (cache != null) {
                okHttpClientBuilder.cache(cache);
            }

            // Add interceptors to enforce
            // A) max-age when GET responses are cached
            // B) max-stale when GET requests are made offline
            removeInterceptor(okHttpClientBuilder.interceptors(), CacheControlOfflineInterceptor.class);
            removeInterceptor(okHttpClientBuilder.networkInterceptors(), CacheControlNetworkInterceptor.class);
            okHttpClientBuilder
                    .addNetworkInterceptor(new CacheControlNetworkInterceptor(maxAgeSeconds))
                    .addInterceptor(new CacheControlOfflineInterceptor(context, okHttpClient.cache() != null));

            // Retry can't hurt? right?
            okHttpClientBuilder.retryOnConnectionFailure(true);

            // This is a major kludge to fix a bug in okhttp3
            // Switching networks will cause old socket connections to not
            // get killed. The workaround is to set the connection pool below.
            // https://github.com/square/okhttp/issues/3146
            okHttpClientBuilder.connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS));

            okHttpClient = okHttpClientBuilder.build();

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

    public static class Utilities {

        private static final String TAG = Utilities.class.getSimpleName();

        public static boolean isNetworkAvailable(Context context) {
            try {
                ConnectivityManager cm =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
            return false;
        }

        public static void logInterfereingHeaders(Response originalResponse, String... interferingHeaders) {
            for (String key : interferingHeaders) {
                if (originalResponse.headers().get(key) != null) {
                    Log.d(TAG, "Header " + key + " " + originalResponse.headers().get(key));
                }
            }
        }

        public static String responseToString(byte[] bytes) {
            String payload = null;
            try {
                if (bytes != null) {
                    payload = new String(bytes, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {

            } catch (Exception e) {

            }
            return payload;
        }

        public static <T> T jsonToGson(final String jsonString, Class<T> clazz) {
            T var = null;
            try {
                Gson gson = new Gson();
                var = gson.fromJson(jsonString, clazz);
            } catch (JsonSyntaxException e) {
                Log.d(TAG, e.getMessage());
            }
            return var;
        }

        public static <T> String gsonToJson(final T data, Class<T> clazz) {
            String var = null;
            try {
                Gson gson = new Gson();
                var = gson.toJson(data, clazz);
            } catch (JsonSyntaxException e) {
                Log.d(TAG, e.getMessage());
            }
            return var;
        }

    }

}
