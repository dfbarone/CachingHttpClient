package com.dfbarone.cachingokhttpclient.simplepersistence;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by hal on 10/24/2017.
 */

public interface SimplePersistenceInterface {
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
    Object load(Request request);
}
