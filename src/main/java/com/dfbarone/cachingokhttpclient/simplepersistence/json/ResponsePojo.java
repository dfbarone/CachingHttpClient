package com.dfbarone.cachingokhttpclient.simplepersistence.json;

/**
 * Created by hal on 10/24/2017.
 */

public class ResponsePojo {

    public String id;
    public String url;
    public String body;
    public String timestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
