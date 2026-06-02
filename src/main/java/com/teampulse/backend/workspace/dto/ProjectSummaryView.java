package com.teampulse.backend.workspace.dto;

public record ProjectSummaryView(
        long projectId,
        String projectName,
        String subject,
        String role,
        String endDate
) {
}
