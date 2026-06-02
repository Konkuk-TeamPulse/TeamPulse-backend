package com.teampulse.backend.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaskCreateSpecRequest(
        @NotBlank(message = "Task title is required.")
        @Size(max = 120, message = "Task title must be 120 characters or fewer.")
        String title,

        @Size(max = 500, message = "Task description must be 500 characters or fewer.")
        String description,

        @NotNull(message = "Assignee id is required.")
        Long assigneeId,

        @NotBlank(message = "Task due date is required.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Task due date must use yyyy-MM-dd.")
        String dueDate
) {
}
