package com.teampulse.backend.workspace.dto;

public record ReportCreateResponse(
        long reportId,
        String downloadUrl
) {
}
