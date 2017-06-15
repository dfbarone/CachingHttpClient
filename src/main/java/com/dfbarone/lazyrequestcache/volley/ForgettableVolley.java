package com.dfbarone.lazyrequestcache.volley;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.dfbarone.lazyrequestcache.okhttp.OkHttp3Stack;

import java.io.File;

/**
 * Created by dbarone on 5/9/2017.
 */

public class ForgettableVolley {

    private final String TAG = ForgettableVolley.class.getName();

    private static final int DEFAULT_DISK_USAGE_BYTES = 10 * 1024 * 1024;
    private static final String DEFAULT_CACHE_DIR = "lazy_volley";

    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, null, DEFAULT_DISK_USAGE_BYTES);
    }

    public static RequestQueue newRequestQueue(Context context, HttpStack stack, int DiskCacheSizeInMB) {
        File cacheDir = new File(getCacheDir(context), DEFAULT_CACHE_DIR);
        cacheDir.mkdirs();
        Cache cache = new DiskBasedCache(cacheDir, DiskCacheSizeInMB);
        if (stack == null) {
            //stack = new HurlStack();
            stack = new OkHttp3Stack();
        }
        Network network = new BasicNetwork(stack);
        RequestQueue queue = new RequestQueue(cache, network);
        queue.start();

        return queue;
    }

    private static File getCacheDir(Context context) {
        File rootCache = context.getExternalCacheDir();
        if (rootCache == null) {
            rootCache = context.getCacheDir();
        }
        return rootCache;
    }

}
