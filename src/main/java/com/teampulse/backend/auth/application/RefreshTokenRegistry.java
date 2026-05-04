package com.teampulse.backend.auth.application;

import com.teampulse.backend.auth.dto.JwtInfo;

public interface RefreshTokenRegistry {

    boolean isActive(String refreshToken);

    void revoke(String refreshToken);

    JwtInfo rotate(String refreshToken);
}
