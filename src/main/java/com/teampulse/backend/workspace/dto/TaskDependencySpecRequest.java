package com.teampulse.backend.workspace.dto;

import jakarta.validation.constraints.NotNull;

public record TaskDependencySpecRequest(
        @NotNull(message = "Preceding task id is required.")
        Long precedingTaskId
) {
}
