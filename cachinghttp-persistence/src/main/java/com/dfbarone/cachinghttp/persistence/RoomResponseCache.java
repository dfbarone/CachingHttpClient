package com.dfbarone.cachinghttp.persistence;

import android.util.Log;

import com.dfbarone.cachinghttp.persistence.room.HttpResponseDao;
import com.dfbarone.cachinghttp.persistence.room.HttpResponseEntry;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 11/6/17.
 */

public class RoomResponseCache implements IResponseCache {

    private static final String TAG = RoomResponseCache.class.getSimpleName();

    private HttpResponseDao httpResponseDao;

    public RoomResponseCache(HttpResponseDao httpResponseDao) {
        this.httpResponseDao = httpResponseDao;
    }

    public synchronized void store(Response response, String responseBody) {
        try {
            if (response.networkResponse() != null && response.isSuccessful()) {
                if (httpResponseDao != null) {
                    HttpResponseEntry entry = new HttpResponseEntry();
                    entry.setUrl(response.request().url().toString());
                    entry.setBody(responseBody);
                    entry.setReceivedResponseAtMillis(response.receivedResponseAtMillis());
                    if (responseBody == null) {
                        Log.d(TAG, "store error");
                    }
                    httpResponseDao.insert(entry);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public synchronized HttpResponseEntry load(Request request) {
        try {
            if (httpResponseDao != null) {
                HttpResponseEntry entry = httpResponseDao.selectResponseByUrl(request.url().toString());
                if (entry.getBody() == null) {
                    Log.d(TAG, "load error");
                }
                return entry;
            }
        } catch (Exception e) {
            Log.d(TAG, "load error");
        }
        return null;
    }
}
