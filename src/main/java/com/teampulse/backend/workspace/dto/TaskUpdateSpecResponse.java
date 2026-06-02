package com.teampulse.backend.workspace.dto;

public record TaskUpdateSpecResponse(
        long taskId,
        String title,
        String description,
        String dueDate
) {
}
