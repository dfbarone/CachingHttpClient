package com.dfbarone.lazyrequestcache.okhttp;

import android.content.Context;

import com.dfbarone.lazyrequestcache.utils.NetworkUtils;

import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by hal on 10/3/2017.
 */

public class OkHttpUtils {

    public static final String WARNING_RESPONSE_IS_STALE = "110";

    public static boolean isResponseStale(Request request, Response response) {
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

    public static Response rewriteCacheControlIfConnected(Context context, Response originalResponse, int maxAge, int maxStale) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            return setOnlineMaxAge(originalResponse, maxAge);
        } else {
            return setOfflineMaxStale(originalResponse, maxStale);
        }
    }

    public static Response setOfflineMaxStale(Response originalResponse, int maxStale) {
        // cache control header for offline persistence
        return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-stale=" + maxStale)
                .build();
    }

    public static Response setOnlineMaxAge(Response originalResponse, int maxAge) {
        // generic cache control header override
        return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + maxAge)
                .build();
    }

    public static String responseToString(Response response) {
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
}
