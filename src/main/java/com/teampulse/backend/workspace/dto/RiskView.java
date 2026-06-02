package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.risk.RiskSeverity;
import java.util.List;

public record RiskView(
        long id,
        RiskSeverity severity,
        String title,
        String body,
        String action,
        List<Long> affectedTaskIds,
        List<String> suggestedActions
) {
    public RiskView(long id, RiskSeverity severity, String title, String body, String action) {
        this(id, severity, title, body, action, List.of(), List.of(action));
    }

    public RiskView {
        affectedTaskIds = affectedTaskIds == null ? List.of() : List.copyOf(affectedTaskIds);
        suggestedActions = suggestedActions == null || suggestedActions.isEmpty()
                ? List.of(action)
                : List.copyOf(suggestedActions);
    }
}
