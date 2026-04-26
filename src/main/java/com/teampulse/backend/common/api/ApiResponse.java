package com.teampulse.backend.common.api;

public record ApiResponse<T>(
        boolean success,
        T data,
        Object meta,
        ApiError error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, Object meta) {
        return new ApiResponse<>(true, data, meta, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, null, new ApiError(code, message, null));
    }

    public static <T> ApiResponse<T> fail(String code, String message, Object details) {
        return new ApiResponse<>(false, null, null, new ApiError(code, message, details));
    }
}
