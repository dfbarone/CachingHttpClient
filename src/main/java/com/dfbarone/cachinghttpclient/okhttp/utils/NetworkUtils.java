package com.dfbarone.cachinghttpclient.okhttp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import okhttp3.Response;

/**
 * Created by hal on 10/1/2017.
 */

public class NetworkUtils {

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
            Log.d(NetworkUtils.class.getSimpleName(), e.getMessage());
        }
        return false;
    }

    public static void logInterfereingHeaders(String TAG, Response originalResponse) {
        if (originalResponse.headers().get("Date") != null) {
            Log.d(TAG, "Header " + "Date" + " " + originalResponse.headers().get("Date"));
        }

        if (originalResponse.headers().get("Expires") != null) {
            Log.d(TAG, "Header " + "Expires" + " " + originalResponse.headers().get("Expires"));
        }

        if (originalResponse.headers().get("Last-Modified") != null) {
            Log.d(TAG, "Header " + "Last-Modified" + " " + originalResponse.headers().get("Last-Modified"));
        }

        if (originalResponse.headers().get("ETag") != null) {
            Log.d(TAG, "ETag " + "Last-Modified" + " " + originalResponse.headers().get("ETag"));
        }

        if (originalResponse.headers().get("Age") != null) {
            Log.d(TAG, "Age " + "Age" + " " + originalResponse.headers().get("Age"));
        }
    }

}
