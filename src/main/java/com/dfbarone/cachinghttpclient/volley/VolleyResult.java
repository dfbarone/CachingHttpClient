package com.dfbarone.cachinghttpclient.volley;

import com.android.volley.VolleyError;

/**
 * Created by dominicbarone on 6/19/17.
 */

public class VolleyResult<T> {
    public T result;
    public VolleyError error;

    public VolleyResult(T result, VolleyError error) {
        this.result = result;
        this.error = error;
    }
}
