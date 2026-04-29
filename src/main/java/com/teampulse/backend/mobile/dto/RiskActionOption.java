package com.teampulse.backend.mobile.dto;

public record RiskActionOption(
        String type,
        String label,
        String description,
        Long targetTaskId,
        String suggestedOwner,
        String suggestedDueDate
) {
}
