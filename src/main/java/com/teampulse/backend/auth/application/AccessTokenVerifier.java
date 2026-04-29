package com.teampulse.backend.auth.application;

public interface AccessTokenVerifier {

    boolean isActiveAccessToken(String accessToken);
}
