package com.teampulse.backend.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskDependencyRequest(
        @NotBlank(message = "Dependency title is required.")
        @Size(max = 120, message = "Dependency title must be 120 characters or fewer.")
        String title
) {
}
