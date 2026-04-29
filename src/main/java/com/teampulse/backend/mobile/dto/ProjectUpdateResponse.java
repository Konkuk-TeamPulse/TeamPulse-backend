package com.teampulse.backend.mobile.dto;

public record ProjectUpdateResponse(
        long projectId,
        String projectName,
        String subject,
        String description,
        String startDate,
        String endDate,
        String updatedAt
) {
}
