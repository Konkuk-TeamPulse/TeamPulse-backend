package com.teampulse.backend.workspace.dto;

public record TeamProfile(
        String name,
        String courseName,
        String semester,
        String dueDate,
        String inviteCode,
        String inviteExpiresAt,
        String description,
        String startDate
) {
    public TeamProfile(
            String name,
            String courseName,
            String semester,
            String dueDate,
            String inviteCode,
            String description,
            String startDate
    ) {
        this(name, courseName, semester, dueDate, inviteCode, "", description, startDate);
    }

    public TeamProfile(String name, String courseName, String semester, String dueDate, String inviteCode) {
        this(name, courseName, semester, dueDate, inviteCode, "", "", "");
    }
}
