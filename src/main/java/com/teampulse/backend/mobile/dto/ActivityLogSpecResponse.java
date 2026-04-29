package com.teampulse.backend.mobile.dto;

public record ActivityLogSpecResponse(
        long logId,
        String action,
        String content,
        String userName,
        String createdAt
) {
}
