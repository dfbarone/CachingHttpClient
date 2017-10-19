package com.dfbarone.cachinghttpclient.okhttp.interceptors;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor to cache data and maintain it max-age
 * <p>
 * If the same network request is sent within a minute,
 * the response is retrieved from cache.
 */
public class CachingNetworkInterceptor implements Interceptor {

    private int maxAgeSeconds;

    public CachingNetworkInterceptor(int maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    /**
     * If there is an invalid max age, intervene and set to 60 seconds.
     * @param chain
     * @return
     * @throws IOException
     */
    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        int maxAge = maxAgeSeconds;
        Request request = chain.request();
        if (request.cacheControl() != null && request.cacheControl().maxAgeSeconds() > -1) {
            maxAge = request.cacheControl().maxAgeSeconds();
        }
        Response originalResponse = chain.proceed(chain.request());
        return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + maxAge)
                .build();
    }
}
