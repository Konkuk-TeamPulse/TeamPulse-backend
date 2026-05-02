package com.teampulse.backend.mobile.dto;

import java.util.List;

public record MeetingDetailResponse(
        long meetingId,
        long projectId,
        String title,
        String meetingDate,
        String agenda,
        String content,
        String decisions,
        List<MeetingAttendeeResponse> attendees,
        List<MeetingActionItemResponse> actionItems,
        String createdAt,
        String updatedAt
) {
}
