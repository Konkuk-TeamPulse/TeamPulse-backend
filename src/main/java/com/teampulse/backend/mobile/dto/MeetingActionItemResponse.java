package com.teampulse.backend.mobile.dto;

public record MeetingActionItemResponse(
        long actionItemId,
        String content,
        Long assigneeMemberId,
        String assigneeName,
        String dueDate,
        boolean isCompleted
) {
}
