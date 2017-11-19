package com.dfbarone.cachingokhttpclient;

import okhttp3.Request;

/**
 * Created by dominicbarone on 11/9/17.
 */

public final class CachingRequest {
    final private Request request;
    final private int maxAgeSeconds;

    CachingRequest(CachingRequest.Builder builder) {
        this.request = builder.request;
        this.maxAgeSeconds = builder.maxAgeSeconds;
    }

    public Request request() {
        return request;
    }

    public int maxAgeSeconds() {
        return maxAgeSeconds;
    }

    @Override
    public String toString() {
        return request.toString();
    }

    public static class Builder {
        Request request;
        int maxAgeSeconds;

        public Builder(Request request) {
            this.request = request;
            this.maxAgeSeconds = -1;
        }

        Builder(CachingRequest cachingRequest) {
            this.request = cachingRequest.request;
            this.maxAgeSeconds = cachingRequest.maxAgeSeconds;
        }

        public CachingRequest.Builder maxAge(int maxAgeSeconds) {
            if (maxAgeSeconds > -1) {
                this.maxAgeSeconds = maxAgeSeconds;
            }
            return this;
        }

        public CachingRequest build() {
            if (request == null) throw new IllegalStateException("request == null");
            return new CachingRequest(this);
        }
    }
}
