package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.task.TaskStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateTaskRequest(
        @Size(max = 120, message = "Task title must be 120 characters or fewer.")
        String title,
        String owner,
        TaskStatus status,
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Task due date must use yyyy-MM-dd.")
        String dueDate,
        String priority,
        List<String> blockers,
        List<String> next,
        String note,
        Long assigneeId
) {
    public UpdateTaskRequest(
            String title,
            String owner,
            TaskStatus status,
            String dueDate,
            String priority,
            List<String> blockers,
            List<String> next,
            String note
    ) {
        this(title, owner, status, dueDate, priority, blockers, next, note, null);
    }
}
