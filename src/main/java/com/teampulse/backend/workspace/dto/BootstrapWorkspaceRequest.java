package com.teampulse.backend.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BootstrapWorkspaceRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 50, message = "Name must be 50 characters or fewer.")
        String name,
        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        String email,
        @NotBlank(message = "Team name is required.")
        @Size(max = 80, message = "Team name must be 80 characters or fewer.")
        String teamName,
        @NotBlank(message = "Course name is required.")
        @Size(max = 80, message = "Course name must be 80 characters or fewer.")
        String courseName,
        String semester,
        @NotBlank(message = "Due date is required.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Due date must use yyyy-MM-dd.")
        String dueDate,
        String description,
        @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "Start date must use yyyy-MM-dd.")
        String startDate
) {
    public BootstrapWorkspaceRequest(
            String name,
            String email,
            String teamName,
            String courseName,
            String semester,
            String dueDate
    ) {
        this(name, email, teamName, courseName, semester, dueDate, "", "");
    }
}
