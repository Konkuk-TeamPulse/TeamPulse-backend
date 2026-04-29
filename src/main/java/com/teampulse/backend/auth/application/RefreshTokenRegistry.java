package com.teampulse.backend.auth.application;

public interface RefreshTokenRegistry {

    boolean isActive(String refreshToken);

    void revoke(String refreshToken);
}
