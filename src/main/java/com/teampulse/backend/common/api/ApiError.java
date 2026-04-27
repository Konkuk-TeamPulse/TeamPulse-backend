package com.teampulse.backend.common.api;

public record ApiError(
        String code,
        String message,
        Object details
) {
}
