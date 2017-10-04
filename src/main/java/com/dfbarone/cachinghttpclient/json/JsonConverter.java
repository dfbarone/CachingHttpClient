package com.dfbarone.cachinghttpclient.json;

import android.util.Log;

import com.google.gson.Gson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Created by dbarone on 5/20/2017.
 */

public class JsonConverter {

    public static final String TAG = JsonConverter.class.getSimpleName();

    public static <T> T moshiFromJson(final String jsonString, Class<T> clazz) {
        T var = null;
        try {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<T> jsonAdapter = moshi.adapter(clazz);
            var = jsonAdapter.fromJson(jsonString);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return var;
    }

    public static <T> T gsonFromJson(final String jsonString, Class<T> clazz) {
        T var = null;
        try {
            Gson gson = new Gson();
            var = gson.fromJson(jsonString, clazz);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return var;
    }
}
