package com.teampulse.backend.mobile.dto;

public record TaskDependencySpecResponse(
        long taskId,
        long precedingTaskId
) {
}
