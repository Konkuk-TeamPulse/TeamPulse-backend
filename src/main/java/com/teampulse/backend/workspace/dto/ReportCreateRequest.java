package com.teampulse.backend.workspace.dto;

import jakarta.validation.constraints.Pattern;

public record ReportCreateRequest(
        @Pattern(regexp = "^$|PDF", message = "Report type must be PDF.")
        String reportType
) {
}
