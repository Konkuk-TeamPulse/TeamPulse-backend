package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.task.TaskStatus;
import java.util.List;

public record TaskView(
        long id,
        String title,
        String owner,
        TaskStatus status,
        String dueDate,
        String priority,
        List<String> blockers,
        List<String> next,
        String note
) {
}
