package com.dfbarone.cachinghttpclient.okhttp.interceptors;

import android.content.Context;

import com.dfbarone.cachinghttpclient.okhttp.utils.NetworkUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;

/**
 * Interceptor to cache data and maintain it for a minute.
 * <p>
 * If the same network request is sent within a minute,
 * the response is retrieved from cache.
 */
public class CachingOfflineInterceptor implements Interceptor {

    private static final int MAX_STALE_SECONDS = 60 * 60 * 24 * 365;
    private Context mContext;

    public CachingOfflineInterceptor(Context context) {
        mContext = context;
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
        if (!NetworkUtils.isNetworkAvailable(mContext)) {
            request = chain.request().newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=" + MAX_STALE_SECONDS)
                    .build();
        }
        return chain.proceed(request);
    }
}