package com.dfbarone.forgettablerequestcache;

import com.android.volley.VolleyError;

import org.json.JSONObject;

/**
 * Created by dbarone on 5/11/2017.
 */

public interface VolleyCallback<T> {
    void onSuccess(T result);
    void onError(VolleyError error);
}
