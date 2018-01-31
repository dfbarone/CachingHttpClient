package com.dfbarone.cachingokhttp;

import com.dfbarone.cachingokhttp.parsing.IResponseParser;

import okhttp3.Request;

/**
 * Created by dfbarone on 11/9/17.
 */

public final class CachingRequest {
    final private Request request;
    final private int maxAgeSeconds;
    final private IResponseParser responseParser;

    CachingRequest(CachingRequest.Builder builder) {
        this.request = builder.request;
        this.maxAgeSeconds = builder.maxAgeSeconds;
        this.responseParser = builder.responseParser;
    }

    public Request request() {
        return request;
    }

    public int maxAgeSeconds() {
        return maxAgeSeconds;
    }

    public IResponseParser responseParser() {
        return responseParser;
    }

    @Override
    public String toString() {
        return request.toString();
    }

    public static class Builder {
        Request request;
        int maxAgeSeconds;
        IResponseParser responseParser;

        public Builder(Request request) {
            this.request = request;
            this.maxAgeSeconds = -1;
            this.responseParser = null;
        }

        Builder(CachingRequest cachingRequest) {
            this.request = cachingRequest.request;
            this.maxAgeSeconds = cachingRequest.maxAgeSeconds;
            this.responseParser = cachingRequest.responseParser;
        }

        public CachingRequest.Builder maxAge(int maxAgeSeconds) {
            if (maxAgeSeconds > -1) {
                this.maxAgeSeconds = maxAgeSeconds;
            }
            return this;
        }

        public CachingRequest.Builder parser(IResponseParser responseParser) {
            this.responseParser = responseParser;
            return this;
        }

        public CachingRequest build() {
            if (request == null) throw new IllegalStateException("request == null");
            return new CachingRequest(this);
        }
    }
}
