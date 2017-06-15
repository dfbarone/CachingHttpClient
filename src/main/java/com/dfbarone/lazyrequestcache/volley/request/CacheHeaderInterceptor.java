package com.dfbarone.lazyrequestcache.volley.request;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;

/**
 * Created by dbarone on 5/11/2017.
 */

public interface CacheHeaderInterceptor {
    Cache.Entry interceptCacheHeader(NetworkResponse response);
}
