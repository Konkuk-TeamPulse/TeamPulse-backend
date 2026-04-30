package com.teampulse.backend.auth.application;

import com.teampulse.backend.auth.domain.AuthUser;
import java.util.Optional;

public interface AccessTokenVerifier {

    boolean isActiveAccessToken(String accessToken);

    Optional<AuthUser> findUserByAccessToken(String accessToken);
}
