package com.dfbarone.lazyrequestcache;

import com.android.volley.Request;
import com.dfbarone.lazyrequestcache.volley.VolleyCallback;

/**
 * Created by dominicbarone on 6/21/17.
 */

public interface IHttpClient {

    public <T> void requestMoshi(final int method,
                                 final String url,
                                 final Class<T> clazz,
                                 final String payload,
                                 final Request.Priority priority,
                                 final int timeout,
                                 final VolleyCallback callback);
}
