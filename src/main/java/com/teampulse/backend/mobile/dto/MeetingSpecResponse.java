package com.teampulse.backend.mobile.dto;

import java.util.List;

public record MeetingSpecResponse(
        long meetingId,
        String title,
        String time,
        String agenda,
        List<String> decisions,
        List<String> actions
) {
    public static MeetingSpecResponse from(MeetingView meeting) {
        return new MeetingSpecResponse(
                meeting.id(),
                meeting.title(),
                meeting.time(),
                meeting.agenda(),
                meeting.decisions(),
                meeting.actions());
    }
}
