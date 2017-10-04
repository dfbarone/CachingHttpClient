package com.dfbarone.cachinghttpclient.volley.request;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dbarone on 5/11/2017.
 */

public class HeaderInterceptorJsonObjectRequest extends JsonObjectRequest {

    private CacheHeaderInterceptor interceptor;
    private Map<String, String> mHeaders;

    /**
     * Creates a new request.
     * @param method the HTTP method to use
     * @param url URL to fetch the JSON from
     * @param jsonRequest A {@link JSONObject} to post with the request. Null is allowed and
     *   indicates no parameters will be posted along with request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public HeaderInterceptorJsonObjectRequest(int method, Map<String, String> headers, String url, JSONObject jsonRequest,
                                              Response.Listener<JSONObject> listener, Response.ErrorListener errorListener,
                                              CacheHeaderInterceptor interceptor) {
        super(method, url, jsonRequest, listener, errorListener);
        this.interceptor = interceptor;
        this.mHeaders = headers;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            Cache.Entry entry = interceptor != null ? interceptor.interceptCacheHeader(response) : HttpHeaderParser.parseCacheHeaders(response);
            JSONObject jsonObject = new JSONObject(jsonString);
            return Response.success(jsonObject, entry);
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headersSys = super.getHeaders();
        Map<String, String> headers = new HashMap<>();
        headersSys.remove("Accept");
        for (String key : mHeaders.keySet()) {
            headers.put(key, mHeaders.get(key));
        }
        headers.putAll(headersSys);
        return headers;
    }
}
