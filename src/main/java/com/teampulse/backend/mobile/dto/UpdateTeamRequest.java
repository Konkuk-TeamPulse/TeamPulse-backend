package com.teampulse.backend.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
        @NotBlank(message = "Team name is required.")
        @Size(max = 80, message = "Team name must be 80 characters or fewer.")
        String name,
        @NotBlank(message = "Course name is required.")
        @Size(max = 80, message = "Course name must be 80 characters or fewer.")
        String courseName,
        String semester,
        @NotBlank(message = "Team due date is required.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Team due date must use yyyy-MM-dd.")
        String dueDate,
        String description,
        @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "Team start date must use yyyy-MM-dd.")
        String startDate
) {
    public UpdateTeamRequest(String name, String courseName, String semester, String dueDate) {
        this(name, courseName, semester, dueDate, "", "");
    }
}
