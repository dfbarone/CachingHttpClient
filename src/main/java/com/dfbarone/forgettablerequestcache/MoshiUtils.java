package com.dfbarone.forgettablerequestcache;

import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONObject;

/**
 * Created by hal on 5/20/2017.
 */

public class MoshiUtils {

    public static final String TAG = MoshiUtils.class.getSimpleName();

    public static <T> T parseJSONObject(final String jsonString, Class<T> clazz) {
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
}
