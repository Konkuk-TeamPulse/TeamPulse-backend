package com.teampulse.backend.workspace.dto;

import java.util.List;

public record MeetingView(
        long id,
        String title,
        String time,
        String agenda,
        String content,
        List<String> decisions,
        List<String> actions,
        List<Long> attendeeIds,
        List<MeetingActionItemView> actionItems,
        String createdAt,
        String updatedAt
) {
    public MeetingView(
            long id,
            String title,
            String time,
            String agenda,
            String content,
            List<String> decisions,
            List<String> actions,
            List<Long> attendeeIds,
            List<MeetingActionItemView> actionItems
    ) {
        this(id, title, time, agenda, content, decisions, actions, attendeeIds, actionItems, timestampFromTime(time), timestampFromTime(time));
    }

    public MeetingView(
            long id,
            String title,
            String time,
            String agenda,
            List<String> decisions,
            List<String> actions
    ) {
        this(id, title, time, agenda, "", decisions, actions, List.of(), actionItemsFromActions(actions));
    }

    private static String timestampFromTime(String time) {
        if (time == null || time.isBlank()) {
            return "";
        }
        return time.contains("T") ? time : time + "T00:00:00";
    }

    private static List<MeetingActionItemView> actionItemsFromActions(List<String> actions) {
        if (actions == null) {
            return List.of();
        }
        return actions.stream()
                .filter(action -> action != null && !action.isBlank())
                .map(action -> new MeetingActionItemView(action, null, null))
                .toList();
    }
}
