package com.dfbarone.cachingokhttp.persistence;

/**
 * Created by dfbarone on 1/17/18.
 */

public interface IResponseCacheEntry {

    public String getUrl();

    public void setUrl(String url);

    public String getBody();

    public void setBody(String body);

    public long getReceivedResponseAtMillis();

    public void setReceivedResponseAtMillis(long receivedResponseAtMillis);

    public String getTag();

    public void setTag(String id);
}
