package com.dfbarone.cachinghttpclient.okhttp;

import android.content.Context;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

/**
 * Created by dominicbarone on 9/12/17.
 */

public class OkHttpClientFactory {

    private final String TAG = OkHttpClientFactory.class.getSimpleName();

    private static final int DEFAULT_DISK_USAGE_BYTES = 10 * 1024 * 1024;
    private static final String DEFAULT_CACHE_DIR = "ok_http_client";

    public static OkHttpClient okHttpClient(Context context) {
        return okHttpClient(context, DEFAULT_DISK_USAGE_BYTES);
    }

    public static OkHttpClient okHttpClient(Context context, int DiskCacheSizeInBytes) {
        File cacheDir = new File(getCacheDir(context), DEFAULT_CACHE_DIR);
        cacheDir.mkdirs();
        Cache cache = new Cache(cacheDir, DiskCacheSizeInBytes);

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
