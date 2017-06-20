package com.dfbarone.lazyrequestcache.okhttp;

import android.content.Context;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

/**
 * Created by dominicbarone on 6/19/17.
 */

public class LazyOkHttp {

    private final String TAG = LazyOkHttp.class.getSimpleName();

    private static final int DEFAULT_DISK_USAGE_BYTES = 10 * 1024 * 1024;
    private static final String DEFAULT_CACHE_DIR = "lazy_okhttp";

    public static OkHttpClient newRequestQueue(Context context) {
        return newRequestQueue(context, DEFAULT_DISK_USAGE_BYTES);
    }

    public static OkHttpClient newRequestQueue(Context context, int DiskCacheSizeInMB) {
        File cacheDir = new File(getCacheDir(context), DEFAULT_CACHE_DIR);
        cacheDir.mkdirs();
        Cache cache = new Cache(cacheDir, DiskCacheSizeInMB);

        OkHttpClient client = new OkHttpClient.Builder()
                .cache(cache)
                .build();

        return client;
    }

    private static File getCacheDir(Context context) {
        File rootCache = context.getExternalCacheDir();
        if (rootCache == null) {
            rootCache = context.getCacheDir();
        }
        return rootCache;
    }

}
