package com.teampulse.backend.auth.dto;

public record LoginResponse(
        long userId,
        String email,
        JwtInfo jwtInfo
) {
}
