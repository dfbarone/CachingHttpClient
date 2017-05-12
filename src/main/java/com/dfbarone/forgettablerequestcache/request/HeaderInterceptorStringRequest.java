package com.dfbarone.forgettablerequestcache.request;

/**
 * Created by dbarone on 5/11/2017.
 */

import java.io.UnsupportedEncodingException;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class HeaderInterceptorStringRequest extends StringRequest {

    private CacheHeaderInterceptor interceptor;

    /**
     * Creates a new request with the given method.
     *
     * @param method        the request {@link Method} to use
     * @param url           URL to fetch the string at
     * @param listener      Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public HeaderInterceptorStringRequest(int method, String url, Listener<String> listener, ErrorListener errorListener, CacheHeaderInterceptor interceptor) {
        super(method, url, listener, errorListener);
        this.interceptor = interceptor;
    }

    /**
     * Creates a new GET request.
     *
     * @param url           URL to fetch the string at
     * @param listener      Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public HeaderInterceptorStringRequest(String url, Listener<String> listener, ErrorListener errorListener, CacheHeaderInterceptor interceptor) {
        super(Method.GET, url, listener, errorListener);
        this.interceptor = interceptor;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        Cache.Entry entry = interceptor != null ? interceptor.interceptCacheHeader(response) : HttpHeaderParser.parseCacheHeaders(response);
        return Response.success(parsed, entry);
    }
}
