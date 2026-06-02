package com.teampulse.backend.workspace.dto;

public record ActivityLogSpecResponse(
        long logId,
        String action,
        String content,
        String userName,
        String createdAt,
        String updatedAt
) {
}
