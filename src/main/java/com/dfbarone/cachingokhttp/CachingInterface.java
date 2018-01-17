package com.dfbarone.cachingokhttp;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 10/24/2017.
 */

public interface CachingInterface {
    /**
     * Store a response anywhere you like
     * @param response
     * @param responseBody
     */
    void store(Response response, String responseBody);

    /**
     * Load a response in any form you like
     * @param request
     * @return
     */
    ResponseEntryInterface load(Request request);
}
