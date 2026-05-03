package com.teampulse.backend.auth.domain;

public record AuthUser(
        long id,
        String email,
        String passwordHash,
        String name,
        String university,
        String phone
) {
}
