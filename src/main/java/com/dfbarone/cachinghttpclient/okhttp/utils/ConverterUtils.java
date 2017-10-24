package com.dfbarone.cachinghttpclient.okhttp.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import okhttp3.Response;

/**
 * Created by hal on 10/17/2017.
 */

public class ConverterUtils {

    public static final String TAG = ConverterUtils.class.getSimpleName();

    public static String responseToString(Response response) {
        String payload = null;
        try {
            if (response != null) {
                payload = new String(response.body().bytes(), "UTF-8");
                response.close();
            }
        } catch (UnsupportedEncodingException e) {

        } catch (IOException e) {

        }
        return payload;
    }

    public static <T> T jsonToMoshi(final String jsonString, Class<T> clazz) {
        T var = null;
        try {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<T> jsonAdapter = moshi.adapter(clazz);
            var = jsonAdapter.fromJson(jsonString);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
        return var;
    }

    public static <T> T jsonToGson(final String jsonString, Class<T> clazz) {
        T var = null;
        try {
            Gson gson = new Gson();
            var = gson.fromJson(jsonString, clazz);
        } catch (JsonSyntaxException e) {
            Log.d(TAG, e.getMessage());
        }
        return var;
    }
}
