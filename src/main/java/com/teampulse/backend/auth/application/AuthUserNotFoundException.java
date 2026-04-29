package com.teampulse.backend.auth.application;

public class AuthUserNotFoundException extends RuntimeException {

    public AuthUserNotFoundException(String email) {
        super("존재하지 않는 회원입니다: " + email);
    }
}
