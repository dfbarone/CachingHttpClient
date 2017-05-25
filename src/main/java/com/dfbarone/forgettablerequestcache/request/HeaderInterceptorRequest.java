package com.dfbarone.forgettablerequestcache.request;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.dfbarone.forgettablerequestcache.MoshiUtils;
import com.dfbarone.forgettablerequestcache.request.CacheHeaderInterceptor;
import com.dfbarone.forgettablerequestcache.request.HeaderInterceptorJsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hal on 5/20/2017.
 */

public class HeaderInterceptorRequest<T> extends JsonRequest<T> {

    private CacheHeaderInterceptor interceptor;
    private final Class<T> clazz;

    /**
     * Creates a new request.
     * @param method the HTTP method to use
     * @param url URL to fetch the JSON from
     * @param jsonRequest A {@link JSONObject} to post with the request. Null is allowed and
     *   indicates no parameters will be posted along with request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public HeaderInterceptorRequest(int method, String url, Class<T> clazz, String jsonRequest,
                                              Response.Listener<T> listener, Response.ErrorListener errorListener,
                                              CacheHeaderInterceptor interceptor) {
        super(method, url, jsonRequest, listener, errorListener);
        this.interceptor = interceptor;
        this.clazz = clazz;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            Cache.Entry entry = interceptor != null ? interceptor.interceptCacheHeader(response) : HttpHeaderParser.parseCacheHeaders(response);
            T convertedPayload = MoshiUtils.parseJSONObject(jsonString, clazz);
            return Response.success(convertedPayload, entry);
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } //catch (JSONException je) {
        //   return Response.error(new ParseError(je));
        //}
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headersSys = super.getHeaders();
        Map<String, String> headers = new HashMap<>();
        headersSys.remove("Accept");
        headers.put("Accept", "application/json; version=3.0");
        headers.put("Content-Type", "application/json");
        headers.putAll(headersSys);
        return headers;
    }

    public static String getCharSet() {
        return PROTOCOL_CHARSET;
    }
}
