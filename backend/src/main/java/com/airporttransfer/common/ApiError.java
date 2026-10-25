package com.airporttransfer.common;

import java.util.Map;

/**
 * The single error format of the API. {@code code} is a stable key the frontend translates;
 * {@code fieldErrors} maps a request field to the violated constraint (e.g. {@code "iataCode": "Pattern"}).
 */
public record ApiError(String code, String message, Map<String, String> fieldErrors) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of());
    }
}
