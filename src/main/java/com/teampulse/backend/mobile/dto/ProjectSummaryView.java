package com.teampulse.backend.mobile.dto;

public record ProjectSummaryView(
        long projectId,
        String projectName,
        String subject,
        String role,
        String endDate
) {
}
