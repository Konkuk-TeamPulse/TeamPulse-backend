package com.teampulse.backend.mobile.dto;

import java.util.List;

public record MeetingSpecResponse(
        long meetingId,
        String title,
        String meetingDate,
        String writerName,
        String agenda,
        String content,
        List<String> decisions,
        List<String> actions,
        List<Long> attendeeIds,
        List<MeetingActionItemView> actionItems
) {
    public static MeetingSpecResponse from(MeetingView meeting) {
        return from(meeting, "");
    }

    public static MeetingSpecResponse from(MeetingView meeting, String writerName) {
        return new MeetingSpecResponse(
                meeting.id(),
                meeting.title(),
                meeting.time(),
                writerName,
                meeting.agenda(),
                meeting.content(),
                meeting.decisions(),
                meeting.actions(),
                meeting.attendeeIds(),
                meeting.actionItems());
    }
}
