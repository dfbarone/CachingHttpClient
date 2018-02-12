package com.dfbarone.cachinghttp.parsing;

import com.google.gson.Gson;

/**
 * Created by dfbarone on 2/1/2018.
 */

public class GsonResponseParser implements IResponseParser {

    private Gson gson;

    public GsonResponseParser(Gson gson) {
        this.gson = gson;
    }

    public <T> T fromString(String responseBody, Class<T> clazz) {
        return gson.fromJson(responseBody, clazz);
    }
}
