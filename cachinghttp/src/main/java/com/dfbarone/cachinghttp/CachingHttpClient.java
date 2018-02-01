package com.dfbarone.cachinghttp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.dfbarone.cachinghttp.interceptors.CacheControlNetworkInterceptor;
import com.dfbarone.cachinghttp.interceptors.CacheControlOfflineInterceptor;
import com.dfbarone.cachinghttp.parsing.IResponseParser;
import com.dfbarone.cachinghttp.persistence.IResponseCache;
import com.dfbarone.cachinghttp.persistence.IResponseCacheEntry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
 * Created by dfbarone on 6/19/17.
 */

public class CachingHttpClient {

    private static final String TAG = CachingHttpClient.class.getSimpleName();
    private OkHttpClient okHttpClient;
    private int maxAgeSeconds;
    private int maxStaleSeconds;
    private Context context;
    private List<IResponseCache> responseDataStores;
    private IResponseParser responseParser;

    public CachingHttpClient(Context context) {
        this.context = context;
        Builder builder = new Builder(context);
        okHttpClient = builder.okHttpClient;
        maxAgeSeconds = builder.maxAgeSeconds;
        maxStaleSeconds = builder.maxStaleSeconds;
        responseDataStores = builder.responseDataStores;
        responseParser = builder.responseParser;
    }

    // For calling inside Builder.build() method
    private CachingHttpClient(Builder builder) {
        this.context = builder.context;
        this.okHttpClient = builder.okHttpClient;
        this.maxAgeSeconds = builder.maxAgeSeconds;
        this.maxStaleSeconds = builder.maxStaleSeconds;
        this.responseDataStores = builder.responseDataStores;
        this.responseParser = builder.responseParser;
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
     * @param interceptors a list of interceptors
     * @param clazz        the class type of new interceptor
     * @param <T>
     */
    private static <T> void removeInterceptor(List<Interceptor> interceptors, Class<T> clazz) {
        for (Interceptor i : interceptors) {
            if (clazz.isInstance(i)) {
                interceptors.remove(i);
            }
        }
    }

    private static void logResponse(Request request, Response response, String prefix) {
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

    public OkHttpClient okHttpClient() {
        return okHttpClient;
    }

    public CachingHttpClient.Builder newBuilder() {
        return new Builder(this);
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
     * @return String response body
     */
    public <T> T get(final CachingRequest cachingRequest, final Class<T> clazz) throws IOException, IllegalArgumentException {
        T payload = null;
        Response response = null;
        try {
            // Make web request
            Call call = newCall(cachingRequest);
            response = call.execute();
            // Log result
            logResponse(cachingRequest.request(), response, "get");
            // Cache data
            if (responseDataStores != null && cachingRequest.request().method().equalsIgnoreCase("get")) {
                store(response);
            }
            // Attempt to parse response body
            payload = parse(cachingRequest, response, clazz);
        } catch (IOException e) {
            Log.d(TAG, "get error " + e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "get error " + e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.d(TAG, "get error " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
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
    public <T> Single<T> getAsync(final CachingRequest cachingRequest, final Class<T> clazz) {
        return Single.fromCallable(() -> get(cachingRequest, clazz));
    }

    private <T> T parse(CachingRequest cachingRequest, Response response, Class<T> clazz) throws IOException {
        T result;
        if (clazz == Response.class) {
            result = (T) response;
        } else if (clazz == String.class) {
            result = (T) response.body().string();
        } else {
            if (cachingRequest.responseParser() != null) {
                result = cachingRequest.responseParser().fromString(response.body().string(), clazz);
            } else {
                result = responseParser.fromString(response.body().string(), clazz);
            }
        }
        return result;
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

            if (response != null && response.cacheResponse() != null && response.isSuccessful()) {
                long diff = (System.currentTimeMillis() - response.receivedResponseAtMillis()) / 1000;
                response.close();
                Log.d(TAG, "isExpired " + (diff > maxAge) + " " + diff + "s");
                return diff > maxAge;
            }
            response.close();

            IResponseCacheEntry responseEntry = load(cachingRequest.request());
            if (responseEntry.getReceivedResponseAtMillis() > 0) {
                long diff = (System.currentTimeMillis() - responseEntry.getReceivedResponseAtMillis()) / 1000;
                response.close();
                Log.d(TAG, "isExpired " + (diff > maxAge) + " " + diff + "s");
                return diff > maxAge;
            }

        } catch (Exception e) {
            Log.d(TAG, "isExpired error " + e.getMessage());
        }
        Log.d(TAG, "isExpired " + true);
        return true;
    }

    private IResponseCacheEntry load(final Request request) {
        if (responseDataStores != null && responseDataStores.size() > 0 && request != null) {
            for (IResponseCache cache : responseDataStores) {
                IResponseCacheEntry entry = cache.load(request);
                if (entry != null) {
                    return entry;
                }
            }
        }
        return null;
    }

    public <T> T load(final CachingRequest cachingRequest, final Class<T> clazz) {
        IResponseCacheEntry dbResponseBody = load(cachingRequest.request());
        if (dbResponseBody != null && dbResponseBody.getBody() != null) {
            if (cachingRequest.responseParser() != null) {
                return cachingRequest.responseParser().fromString(dbResponseBody.getBody(), clazz);
            } else if (responseParser != null) {
                return responseParser.fromString(dbResponseBody.getBody(), clazz);
            }
        }
        return null;
    }

    public void store(Response response) throws IOException {
        if (responseDataStores != null && responseDataStores.size() > 0 && response != null) {
            String body = response.peekBody(Long.MAX_VALUE).string();
            if (body != null) {
                for (IResponseCache cache : responseDataStores) {
                    cache.store(response, body);
                }
            }
        }
    }

    public static final class Builder {

        public static final int MAX_AGE_SECONDS = 60;
        public static final int MAX_STALE_SECONDS = 60 * 60 * 24 * 356;
        private static final int DEFAULT_DISK_SIZE_BYTES = 10 * 1024 * 1024;
        private static final String DEFAULT_CACHE_DIR = "caching_ok_http_client";

        private OkHttpClient okHttpClient;
        private Context context;
        private Cache okHttpClientCache;
        private List<IResponseCache> responseDataStores;
        private IResponseParser responseParser;
        private int maxAgeSeconds;
        private int maxStaleSeconds;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
            this.okHttpClient = null;
            this.okHttpClientCache = null;
            this.responseDataStores = new ArrayList<>();
            this.responseParser = null;
            this.maxAgeSeconds = MAX_AGE_SECONDS;
            this.maxStaleSeconds = MAX_STALE_SECONDS;
        }

        public Builder(CachingHttpClient cachingHttpClient) {
            this.context = cachingHttpClient.context;
            this.okHttpClient = cachingHttpClient.okHttpClient();
            this.okHttpClientCache = null;
            this.responseDataStores = new ArrayList<>();
            this.responseParser = null;
            this.maxAgeSeconds = cachingHttpClient.maxAgeSeconds;
            this.maxStaleSeconds = cachingHttpClient.maxStaleSeconds;
        }

        /**
         * Sets the okhttp response cache to be used to read and write cached responses.
         */
        public Builder cache(Cache httpCache) {
            this.okHttpClientCache = httpCache;
            return this;
        }

        /**
         * Sets the okhttp response cache to be used to read and write cached responses.
         */
        public Builder cache(String cacheDirectory, int diskSizeInBytes) {
            this.okHttpClientCache = CacheHelper.createCache(context, cacheDirectory, diskSizeInBytes);
            return this;
        }

        /**
         * Sets the okhttp response cache to be used to read and write cached responses.
         */
        public Builder cache() {
            this.okHttpClientCache = CacheHelper.createCache(context, DEFAULT_CACHE_DIR, DEFAULT_DISK_SIZE_BYTES);
            return this;
        }

        /**
         * Sets the response data store to persist responses.
         */
        public Builder dataStore(IResponseCache dataStore) {
            this.responseDataStores.add(dataStore);
            return this;
        }

        /**
         * Sets the default response parser.
         */
        public Builder parser(IResponseParser responseParser) {
            this.responseParser = responseParser;
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

        public CachingHttpClient build() {

            // If no default ok http client, make one.
            if (okHttpClient == null) {
                throw new IllegalArgumentException("OkHttpClient cannot be null");
            }

            OkHttpClient.Builder okHttpClientBuilder = okHttpClient.newBuilder();

            // If cache has been set, override.
            if (okHttpClientCache != null) {
                okHttpClientBuilder.cache(okHttpClientCache);
            } else {
                cache();
                okHttpClientBuilder.cache(okHttpClientCache);
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

            return new CachingHttpClient(this);
        }

        public static class CacheHelper {
            /*
             * Utility methods
             */
            public static Cache createCache(Context context, String cacheDirName, int diskCacheSizeInBytes) {
                File cacheDir = new File(tryGetExternalCacheDir(context), cacheDirName);
                cacheDir.mkdirs();
                return new Cache(cacheDir, diskCacheSizeInBytes);
            }

            private static File tryGetExternalCacheDir(Context context) {
                File rootCache = context.getExternalCacheDir();
                if (rootCache == null) {
                    rootCache = context.getCacheDir();
                }
                return rootCache;
            }
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

    }

}
