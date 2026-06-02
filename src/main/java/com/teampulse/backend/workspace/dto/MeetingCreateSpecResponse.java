package com.teampulse.backend.workspace.dto;

public record MeetingCreateSpecResponse(
        long meetingId,
        String title,
        String meetingDate
) {
}
