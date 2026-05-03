package com.teampulse.backend.auth.application;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("중복된 이메일입니다: " + email);
    }
}
