package com.teampulse.backend.mobile.dto;

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
