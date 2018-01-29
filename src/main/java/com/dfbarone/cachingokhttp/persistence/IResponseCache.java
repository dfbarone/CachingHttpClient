package com.dfbarone.cachingokhttp.persistence;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dfbarone on 10/24/2017.
 */

public interface IResponseCache {
    /**
     * Load a response
     *
     * @param request
     * @return
     */
    IResponseCacheEntry load(Request request);

    /**
     * Store a response
     *
     * @param response
     * @param responseBody
     */
    void store(Response response, String responseBody);
}
