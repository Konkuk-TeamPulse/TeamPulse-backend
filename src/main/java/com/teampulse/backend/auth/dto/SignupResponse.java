package com.teampulse.backend.auth.dto;

public record SignupResponse(
        long userId,
        String email,
        String name,
        String university,
        String phone,
        JwtInfo jwtInfo
) {
}
