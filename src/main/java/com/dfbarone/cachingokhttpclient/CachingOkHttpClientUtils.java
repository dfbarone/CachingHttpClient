package com.dfbarone.cachingokhttpclient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dominicbarone on 10/1/2017.
 */

public class CachingOkHttpClientUtils {

    private static final String TAG = CachingOkHttpClientUtils.class.getSimpleName();

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.d(CachingOkHttpClientUtils.class.getSimpleName(), e.getMessage());
        }
        return false;
    }

    public static void logResponse(Request request, Response response, String prefix) {
        try {
            if (response.networkResponse() != null && response.cacheResponse() != null) {
                Log.d(TAG, prefix + "  cond'tnl " + response.networkResponse().code() + " " + request.url());
            } else if (response.networkResponse() != null) {
                Log.d(TAG, prefix + "   network " + response.networkResponse().code() + " " + request.url());
            } else if (response.cacheResponse() != null) {
                long diff = (System.currentTimeMillis() - response.receivedResponseAtMillis()) / 1000;
                Log.d(TAG, prefix + "    cached " + response.cacheResponse().code() + " " + diff + "s old" + " " + request.url());
            } else {
                Log.d(TAG, prefix + " not found " + response.code() + " " + response.message() + " " + request.url());
            }
        } catch (Exception e) {

        }
    }

    public static void logInterfereingHeaders(Response originalResponse, String... interferingHeaders) {
        for (String key : interferingHeaders) {
            if (originalResponse.headers().get(key) != null) {
                Log.d(TAG, "Header " + key + " " + originalResponse.headers().get(key));
            }
        }
    }

    public static String responseToString(byte[] bytes) {
        String payload = null;
        try {
            if (bytes != null) {
                payload = new String(bytes, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {

        } catch (Exception e) {

        }
        return payload;
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

    public static <T> String gsonToJson(final T data, Class<T> clazz) {
        String var = null;
        try {
            Gson gson = new Gson();
            var = gson.toJson(data, clazz);
        } catch (JsonSyntaxException e) {
            Log.d(TAG, e.getMessage());
        }
        return var;
    }

}
