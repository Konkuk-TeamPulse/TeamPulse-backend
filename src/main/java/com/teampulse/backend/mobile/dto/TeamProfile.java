package com.teampulse.backend.mobile.dto;

public record TeamProfile(
        String name,
        String courseName,
        String semester,
        String dueDate,
        String inviteCode,
        String description,
        String startDate
) {
    public TeamProfile(String name, String courseName, String semester, String dueDate, String inviteCode) {
        this(name, courseName, semester, dueDate, inviteCode, "", "");
    }
}
