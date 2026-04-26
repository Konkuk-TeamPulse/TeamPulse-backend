package com.teampulse.backend.mobile.dto;

import java.util.List;

public record MeetingView(
        long id,
        String title,
        String time,
        String agenda,
        List<String> decisions,
        List<String> actions
) {
}
