package com.teampulse.backend.mobile.dto;

import java.util.List;

public record MeetingSpecResponse(
        long meetingId,
        String title,
        String time,
        String agenda,
        String content,
        List<String> decisions,
        List<String> actions,
        List<Long> attendeeIds,
        List<MeetingActionItemView> actionItems
) {
    public static MeetingSpecResponse from(MeetingView meeting) {
        return new MeetingSpecResponse(
                meeting.id(),
                meeting.title(),
                meeting.time(),
                meeting.agenda(),
                meeting.content(),
                meeting.decisions(),
                meeting.actions(),
                meeting.attendeeIds(),
                meeting.actionItems());
    }
}
