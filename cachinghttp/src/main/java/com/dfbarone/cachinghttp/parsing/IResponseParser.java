package com.dfbarone.cachingokhttp.parsing;

/**
 * Created by dfbarone on 1/28/2018.
 */

public interface IResponseParser {
    /**
     * Deserialize a String to type T
     *
     * @param responseBody
     * @param clazz
     * @return T
     */
    <T> T fromString(String responseBody, Class<T> clazz);
}
