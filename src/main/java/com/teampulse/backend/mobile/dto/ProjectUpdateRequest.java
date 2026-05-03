package com.teampulse.backend.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
        @NotBlank(message = "Project name is required.")
        @Size(max = 80, message = "Project name must be 80 characters or fewer.")
        String projectName,

        @NotBlank(message = "Subject is required.")
        @Size(max = 80, message = "Subject must be 80 characters or fewer.")
        String subject,

        @Size(max = 500, message = "Description must be 500 characters or fewer.")
        String description,

        @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "Start date must use yyyy-MM-dd.")
        String startDate,

        @NotBlank(message = "End date is required.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "End date must use yyyy-MM-dd.")
        String endDate
) {
}
