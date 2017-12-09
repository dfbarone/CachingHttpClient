package com.dfbarone.cachingokhttpclient.simplepersistence;

import android.content.SharedPreferences;
import android.util.Log;

import com.dfbarone.cachingokhttpclient.simplepersistence.json.ResponsePojo;
import com.google.gson.Gson;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 10/24/2017.
 */

public class SharedPreferencesDataStore implements SimplePersistenceInterface {

    private static final String TAG = SharedPreferencesDataStore.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    public SharedPreferencesDataStore(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public synchronized void store(Response response, String responseBody) {
        try {
            if (response.networkResponse() != null && response.networkResponse().isSuccessful()) {
                if (sharedPreferences != null) {

                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    ResponsePojo pojo = new ResponsePojo();
                    pojo.setId(response.request().url().toString());
                    pojo.setUrl(pojo.getId());
                    pojo.setBody(responseBody);
                    pojo.setTimestamp(String.valueOf(response.receivedResponseAtMillis()));

                    Gson gson = new Gson();
                    String json = gson.toJson(pojo, ResponsePojo.class).toString();

                    editor.putString(pojo.getId(), json);
                    editor.apply();

                    Log.d(TAG, "store " + pojo.getUrl());
                    Log.d(TAG, "store " + responseBody);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public synchronized ResponsePojo load(Request request) {
        try {
            if (sharedPreferences != null) {
                final String body = sharedPreferences.getString(request.url().toString(), "");

                Gson gson = new Gson();
                ResponsePojo pojo = gson.fromJson(body, ResponsePojo.class);

                Log.d(TAG, "load " + " " + pojo.getUrl());
                Log.d(TAG, "load " + " " + body);
                return pojo;
            }
        } catch (Exception e) {
            Log.d(TAG, "load error");
        }
        return null;
    }

}
