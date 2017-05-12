package com.dfbarone.forgettablerequestcache;

import com.android.volley.VolleyError;

import org.json.JSONObject;

/**
 * Created by dbarone on 5/11/2017.
 */

public interface VolleyCallback {
    void onSuccess(JSONObject result);
    void onError(VolleyError error);
}
