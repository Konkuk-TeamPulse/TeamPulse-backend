package com.teampulse.backend.mobile.dto;

public record ProjectCreateResponse(
        long projectId,
        String projectName,
        String role
) {
}
