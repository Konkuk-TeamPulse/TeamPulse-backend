package com.teampulse.backend.auth.application;

public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException() {
        super("Too many login attempts.");
    }
}
