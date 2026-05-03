package com.teampulse.backend.auth.application;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("\uC798\uBABB\uB41C Refresh \uD1A0\uD070\uC785\uB2C8\uB2E4.");
    }
}
