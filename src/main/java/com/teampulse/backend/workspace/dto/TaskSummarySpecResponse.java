package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.task.TaskStatus;
import java.util.List;

public record TaskSummarySpecResponse(
        long taskId,
        String title,
        TaskStatus status,
        Long assigneeId,
        String assigneeName,
        String assigneeEmail,
        String dueDate,
        List<Long> precedingTaskIds,
        List<Long> blockedTaskIds
) {
}
