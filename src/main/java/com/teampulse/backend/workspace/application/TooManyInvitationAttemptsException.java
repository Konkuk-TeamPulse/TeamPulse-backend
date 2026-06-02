package com.teampulse.backend.workspace.application;

public class TooManyInvitationAttemptsException extends RuntimeException {

    public TooManyInvitationAttemptsException() {
        super("Too many invalid invitation attempts.");
    }
}
