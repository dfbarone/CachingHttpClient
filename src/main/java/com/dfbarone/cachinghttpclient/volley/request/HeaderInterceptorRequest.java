package com.dfbarone.cachinghttpclient.volley.request;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.dfbarone.cachinghttpclient.json.JsonConverter;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dbarone on 5/20/2017.
 */

public class HeaderInterceptorRequest<T> extends JsonRequest<T> {

    private CacheHeaderInterceptor interceptor;
    private final Class<T> clazz;
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
    public HeaderInterceptorRequest(int method, Map<String,String> header, String url, Class<T> clazz, String jsonRequest,
                                              Response.Listener<T> listener, Response.ErrorListener errorListener,
                                              CacheHeaderInterceptor interceptor) {
        super(method, url, jsonRequest, listener, errorListener);
        this.interceptor = interceptor;
        this.clazz = clazz;
        this.mHeaders = header;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            Cache.Entry entry = interceptor != null ? interceptor.interceptCacheHeader(response) : HttpHeaderParser.parseCacheHeaders(response);
            T convertedPayload = JsonConverter.gsonFromJson(jsonString, clazz);
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
        for (String key : mHeaders.keySet()) {
            headers.put(key, mHeaders.get(key));
        }
        headers.putAll(headersSys);
        return headers;
    }

    public static String getCharSet() {
        return PROTOCOL_CHARSET;
    }
}
