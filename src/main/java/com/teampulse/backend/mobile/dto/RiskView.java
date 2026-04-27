package com.teampulse.backend.mobile.dto;

import com.teampulse.backend.domain.risk.RiskSeverity;

public record RiskView(long id, RiskSeverity severity, String title, String body, String action) {
}
