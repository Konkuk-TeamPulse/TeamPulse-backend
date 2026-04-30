package com.teampulse.backend.mobile.dto;

public record MeetingActionItemView(
        String content,
        Long assigneeId,
        String dueDate
) {
}
