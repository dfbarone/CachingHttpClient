package com.dfbarone.cachingokhttp.persistence;

/**
 * Created by dfbarone on 10/24/2017.
 */

public class ResponseCacheEntry implements IResponseCacheEntry {

    public String url = "";
    public String body = "";
    public long receivedResponseAtMillis = -1;
    public String tag = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getReceivedResponseAtMillis() {
        return receivedResponseAtMillis;
    }

    public void setReceivedResponseAtMillis(long timestamp) {
        this.receivedResponseAtMillis = timestamp;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
