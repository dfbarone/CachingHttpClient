package com.dfbarone.cachinghttpclient.simplepersistence;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by hal on 10/24/2017.
 */

public interface SimplePersistenceInterface {
    /**
     * Store a response anywhere you like
     * @param request
     * @param response
     * @param body
     */
    void store(Request request, Response response, String body);

    /**
     * Load a response in any form you like
     * @param request
     * @return
     */
    String load(Request request);
}
