package com.dfbarone.cachinghttpclient.volley.request;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.Map;

/**
 * Created by dbarone on 5/12/2017.
 */

public class LazyRequestCacheInterceptor implements CacheHeaderInterceptor {

    private static final int DEFAULT_SOFT_TTL = 1 * 60 * 1000; // 1m
    private static final int DEFAULT_TTL = 30 * 24 * 60 * 60 * 1000; // 1month

    private int mSoftTTL;
    private int mTTL;

    public LazyRequestCacheInterceptor() {
        mSoftTTL = DEFAULT_SOFT_TTL;
        mTTL = DEFAULT_TTL;
    }

    public LazyRequestCacheInterceptor(int softTTL, int TTL) {
        mSoftTTL = softTTL;
        mTTL = TTL;
    }

    public Cache.Entry interceptCacheHeader(NetworkResponse response) {
        long now = System.currentTimeMillis();

        Map<String, String> headers = response.headers;
        long serverDate = 0;
        String serverEtag = null;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
            serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }

        serverEtag = headers.get("ETag");

        final long cacheHitButRefreshed = mSoftTTL;
        final long cacheExpired = mTTL;
        final long softExpire = now + cacheHitButRefreshed;
        final long ttl = now + cacheExpired;

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;

        return entry;
    }
}
