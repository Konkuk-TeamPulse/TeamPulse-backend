package com.teampulse.backend.mobile.dto;

public record TaskUpdateSpecResponse(
        long taskId,
        String title,
        String description,
        String dueDate
) {
}
