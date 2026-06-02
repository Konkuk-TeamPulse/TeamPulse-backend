package com.teampulse.backend.workspace.dto;

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
