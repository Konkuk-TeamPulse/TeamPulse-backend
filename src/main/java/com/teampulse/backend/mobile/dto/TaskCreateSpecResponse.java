package com.teampulse.backend.mobile.dto;

import com.teampulse.backend.domain.task.TaskStatus;

public record TaskCreateSpecResponse(
        long taskId,
        String title,
        TaskStatus status,
        long assigneeId,
        String dueDate
) {
}
