package com.dfbarone.cachingokhttp;

import android.content.Context;

import com.dfbarone.cachingokhttp.CachingOkHttpClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;

/**
 * Interceptor to cache data and maintain it for a minute.
 * <p>
 * If the same network request is sent within a minute,
 * the response is retrieved from cache.
 */
public class CacheControlOfflineInterceptor implements Interceptor {

    private static final int MAX_STALE_SECONDS = 60 * 60 * 24 * 365;
    private Context mContext;
    private boolean mHasCache;

    public CacheControlOfflineInterceptor(Context context, boolean hasCache) {
        mContext = context;
        mHasCache = hasCache;
    }

    /**
     * If offline, request based on max-stale
     * @param chain
     * @return
     * @throws IOException
     */
    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!CachingOkHttpClient.Utilities.isNetworkAvailable(mContext) && mHasCache) {
            if (request.method().equalsIgnoreCase("get")) {
                request = chain.request().newBuilder()
                        //.header("Cache-Control", "public, only-if-cached, max-stale=" + MAX_STALE_SECONDS)
                        .cacheControl(new CacheControl.Builder()
                                .maxStale(MAX_STALE_SECONDS, TimeUnit.SECONDS)
                                .onlyIfCached()
                                .build())
                        .build();
            }
        }
        return chain.proceed(request);
    }
}
