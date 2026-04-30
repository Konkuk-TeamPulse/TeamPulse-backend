package com.teampulse.backend.mobile.dto;

import com.teampulse.backend.domain.task.TaskStatus;

public record TaskSummarySpecResponse(
        long taskId,
        String title,
        TaskStatus status,
        String assigneeName,
        String dueDate
) {
}
