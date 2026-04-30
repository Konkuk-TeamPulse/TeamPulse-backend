package com.teampulse.backend.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateTaskRequest(
        @NotBlank(message = "Task title is required.")
        @Size(max = 120, message = "Task title must be 120 characters or fewer.")
        String title,
        @NotBlank(message = "Task owner is required.")
        String owner,
        @NotBlank(message = "Task due date is required.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Task due date must use yyyy-MM-dd.")
        String dueDate,
        List<String> blockers,
        String note
) {
    public CreateTaskRequest(String title, String owner, String dueDate, List<String> blockers) {
        this(title, owner, dueDate, blockers, "");
    }
}
