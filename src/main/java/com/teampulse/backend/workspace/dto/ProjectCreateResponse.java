package com.teampulse.backend.workspace.dto;

public record ProjectCreateResponse(
        long projectId,
        String projectName,
        String role
) {
}
