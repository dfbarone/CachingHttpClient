package com.dfbarone.cachinghttp.persistence.room;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import com.dfbarone.cachinghttp.persistence.IResponseCacheEntry;

/**
 * Created by dominicbarone on 11/6/17.
 */

@Entity(tableName = "http_response", indices = {@Index(value ={"url"}, unique = true)})
public class HttpResponseEntry implements IResponseCacheEntry {

    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "url")
    private String url;

    @ColumnInfo(name = "body")
    private String body;

    @ColumnInfo(name = "receivedResponseAtMillis")
    private long receivedResponseAtMillis;

    @ColumnInfo(name = "tag")
    private String tag;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
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

    public long getReceivedResponseAtMillis() {
        return receivedResponseAtMillis;
    }

    public void setReceivedResponseAtMillis(long receivedResponseAtMillis) {
        this.receivedResponseAtMillis = receivedResponseAtMillis;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }
}
