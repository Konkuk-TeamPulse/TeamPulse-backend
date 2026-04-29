package com.teampulse.backend.mobile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaskUpdateSpecRequest(
        @Size(max = 120, message = "Task title must be 120 characters or fewer.")
        String title,

        @Size(max = 500, message = "Task description must be 500 characters or fewer.")
        String description,

        Long assigneeId,

        @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "Task due date must use yyyy-MM-dd.")
        String dueDate
) {
}
