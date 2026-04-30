package com.teampulse.backend.common.api;

public record SpecResponse<T>(
        boolean isSuccess,
        int responseCode,
        String responseMessage,
        T result
) {
    public static <T> SpecResponse<T> ok(String message, T result) {
        return new SpecResponse<>(true, 1000, message, result);
    }

    public static <T> SpecResponse<T> fail(int responseCode, String message, T result) {
        return new SpecResponse<>(false, responseCode, message, result);
    }
}
