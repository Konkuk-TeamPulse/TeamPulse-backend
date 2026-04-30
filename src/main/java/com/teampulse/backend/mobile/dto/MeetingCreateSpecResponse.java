package com.teampulse.backend.mobile.dto;

public record MeetingCreateSpecResponse(
        long meetingId,
        String title,
        String meetingDate
) {
}
