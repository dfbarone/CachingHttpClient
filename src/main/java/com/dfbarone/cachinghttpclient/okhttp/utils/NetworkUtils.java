package com.dfbarone.cachinghttpclient.okhttp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

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

}
