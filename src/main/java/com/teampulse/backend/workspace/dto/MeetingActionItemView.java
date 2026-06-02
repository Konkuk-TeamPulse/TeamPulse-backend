package com.teampulse.backend.workspace.dto;

public record MeetingActionItemView(
        String content,
        Long assigneeId,
        String dueDate
) {
}
