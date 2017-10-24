package com.dfbarone.cachinghttpclient.simplepersistence;

import android.content.SharedPreferences;
import android.util.Log;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by hal on 10/24/2017.
 */

public class SharedPreferencesDataStore implements SimplePersistenceInterface {

    private static final String TAG = SharedPreferencesDataStore.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    public SharedPreferencesDataStore(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public synchronized void store(Request request, Response response, String body) {
        try {
            if (sharedPreferences != null) {
                String url = request.url().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(url, body);
                editor.commit();
                Log.d(TAG, "store " + url);
                Log.d(TAG, "store " + body);

               // load(url);
            }
        } catch (Exception e) {
            Log.d(TAG, "store error");
        }
    }

    public synchronized String load(Request request) {
        try {
            if (sharedPreferences != null) {
                final String body = sharedPreferences.getString(request.url().toString(), "");
                Log.d(TAG, "load " + " " + request.url().toString());
                Log.d(TAG, "load " + " " + body);
                return body;
            }
        } catch (Exception e) {
            Log.d(TAG, "load error");
        }
        return "";
    }

    public synchronized void clear() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().commit();
        }
    }
}
