package com.teampulse.backend.workspace.dto;

public record TaskDependencySpecResponse(
        long taskId,
        long precedingTaskId,
        String taskTitle,
        String precedingTaskTitle
) {
}
