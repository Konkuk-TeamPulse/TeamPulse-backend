package com.teampulse.backend.mobile.dto;

public record UserMeResponse(
        long userId,
        String email,
        String studentId,
        String name,
        String university,
        String phone
) {
}
