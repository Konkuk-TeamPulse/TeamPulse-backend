package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull(message = "Task status is required.") TaskStatus status) {
}
