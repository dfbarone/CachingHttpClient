package com.dfbarone.cachinghttpclient.okhttp.interceptors;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

    public static final String TAG = CachingNetworkInterceptor.class.getSimpleName();
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
        Request request = chain.request();
        Response originalResponse = chain.proceed(request);
        // Only modify response if this is a GET request
        if (request.method().equalsIgnoreCase("get")) {
            // Override cache control header. Using CacheControl builder hoping it may be more
            // forward compatible than hard coding '"max-age=" + maxAgeSeconds'
            return originalResponse.newBuilder()
                    .header("Cache-Control", new CacheControl.Builder()
                            .maxAge(maxAgeSeconds, TimeUnit.SECONDS)
                            .build()
                            .toString())
                    .build();
        }
        return originalResponse;
    }
}
