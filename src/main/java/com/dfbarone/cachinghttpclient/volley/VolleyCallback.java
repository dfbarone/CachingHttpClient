package com.dfbarone.cachinghttpclient.volley;

import com.android.volley.VolleyError;

/**
 * Created by dbarone on 5/11/2017.
 */

public interface VolleyCallback<T> {
    void onSuccess(T result);
    void onError(VolleyError error, T result);
}
