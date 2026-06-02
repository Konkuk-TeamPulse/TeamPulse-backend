package com.teampulse.backend.workspace.dto;

public record UserMeResponse(
        long userId,
        String email,
        String studentId,
        String name,
        String university,
        String phone
) {
}
