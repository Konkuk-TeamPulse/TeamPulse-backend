package com.teampulse.backend.mobile.dto;

import jakarta.validation.constraints.NotNull;

public record TaskDependencySpecRequest(
        @NotNull(message = "Preceding task id is required.")
        Long precedingTaskId
) {
}
