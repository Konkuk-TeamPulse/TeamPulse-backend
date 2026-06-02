package com.teampulse.backend.workspace.dto;

public record ProjectDetailView(
        long projectId,
        String projectName,
        String subject,
        String description,
        String startDate,
        String endDate,
        int memberCount
) {
}
