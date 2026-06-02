package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.task.TaskStatus;

public record TaskStatusSpecResponse(
        long taskId,
        TaskStatus status
) {
}
