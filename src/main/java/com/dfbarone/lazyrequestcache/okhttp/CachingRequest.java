package com.dfbarone.lazyrequestcache.okhttp;

/**
 * Created by dominicbarone on 9/28/17.
 */

import java.util.List;

import okhttp3.Headers;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okhttp3.internal.http.HttpMethod;

/**
 * An HTTP request. Instances of this class are immutable if their {@link #body} is null or itself
 * immutable.
 */
public final class CachingRequest {
    final private String url;
    final private String method;
    final private Headers headers;
    final private RequestBody body;
    final private Object tag;
    final private boolean isCacheOnly;
    final private long maxAge;
    final private long maxStale;

    CachingRequest(CachingRequest.Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers.build();
        this.body = builder.body;
        this.tag = builder.tag != null ? builder.tag : this;
        this.isCacheOnly = builder.isCacheOnly;
        this.maxAge = builder.maxAge;
        this.maxStale = builder.maxStale;
    }

    public String url() {
        return url;
    }

    public String method() {
        return method;
    }

    public Headers headers() {
        return headers;
    }

    public String header(String name) {
        return headers.get(name);
    }

    public List<String> headers(String name) {
        return headers.values(name);
    }

    public RequestBody body() {
        return body;
    }

    public Object tag() {
        return tag;
    }

    public boolean isCacheOnly() {
        return isCacheOnly;
    }

    public long maxAge() {
        return maxAge;
    }

    public long maxStale() {
        return maxStale;
    }

    public CachingRequest.Builder newBuilder() {
        return new CachingRequest.Builder(this);
    }

    public static class Builder {
        String url;
        String method;
        Headers.Builder headers;
        RequestBody body;
        Object tag;
        boolean isCacheOnly;
        long maxAge;
        long maxStale;

        public Builder() {
            this.method = "GET";
            this.headers = new Headers.Builder();
            isCacheOnly = false;
            maxAge = 0;
            maxStale = 0;
        }

        Builder(CachingRequest request) {
            this.url = request.url;
            this.method = request.method;
            this.body = request.body;
            this.tag = request.tag;
            this.headers = request.headers.newBuilder();
            isCacheOnly = request.isCacheOnly;
            maxAge = request.maxAge;
            maxStale = request.maxStale;
        }

        public CachingRequest.Builder url(String url) {
            if (url == null) throw new NullPointerException("url == null");
            this.url = url;
            return this;
        }

        /**
         * Sets the header named {@code name} to {@code value}. If this request already has any headers
         * with that name, they are all replaced.
         */
        public CachingRequest.Builder header(String name, String value) {
            headers.set(name, value);
            return this;
        }

        /**
         * Adds a header with {@code name} and {@code value}. Prefer this method for multiply-valued
         * headers like "Cookie".
         *
         * <p>Note that for some headers including {@code Content-Length} and {@code Content-Encoding},
         * OkHttp may replace {@code value} with a header derived from the request body.
         */
        public CachingRequest.Builder addHeader(String name, String value) {
            headers.add(name, value);
            return this;
        }

        public CachingRequest.Builder removeHeader(String name) {
            headers.removeAll(name);
            return this;
        }

        /** Removes all headers on this builder and adds {@code headers}. */
        public CachingRequest.Builder headers(Headers headers) {
            this.headers = headers.newBuilder();
            return this;
        }

        public CachingRequest.Builder isCacheOnly(boolean isCacheOnly) {
            this.isCacheOnly = isCacheOnly;
            return this;
        }

        public CachingRequest.Builder maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public CachingRequest.Builder maxStale(int maxStale) {
            this.maxStale = maxStale;
            return this;
        }

        public CachingRequest.Builder get() {
            return method("GET", null);
        }

        public CachingRequest.Builder head() {
            return method("HEAD", null);
        }

        public CachingRequest.Builder post(RequestBody body) {
            return method("POST", body);
        }

        public CachingRequest.Builder delete(RequestBody body) {
            return method("DELETE", body);
        }

        public CachingRequest.Builder delete() {
            return delete(Util.EMPTY_REQUEST);
        }

        public CachingRequest.Builder put(RequestBody body) {
            return method("PUT", body);
        }

        public CachingRequest.Builder patch(RequestBody body) {
            return method("PATCH", body);
        }

        public CachingRequest.Builder method(String method, RequestBody body) {
            if (method == null) throw new NullPointerException("method == null");
            if (method.length() == 0) throw new IllegalArgumentException("method.length() == 0");
            if (body != null && !HttpMethod.permitsRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must not have a request body.");
            }
            if (body == null && HttpMethod.requiresRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must have a request body.");
            }
            this.method = method;
            this.body = body;
            return this;
        }

        /**
         * Attaches {@code tag} to the request. It can be used later to cancel the request. If the tag
         * is unspecified or null, the request is canceled by using the request itself as the tag.
         */
        public CachingRequest.Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public CachingRequest build() {
            if (url == null) throw new IllegalStateException("url == null");
            return new CachingRequest(this);
        }
    }
}
