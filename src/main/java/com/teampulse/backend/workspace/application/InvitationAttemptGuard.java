package com.teampulse.backend.workspace.application;

import com.teampulse.backend.common.security.AttemptRateLimiter;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class InvitationAttemptGuard {

    private static final int MAX_FAILED_ATTEMPTS = 20;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(10);

    private final AttemptRateLimiter rateLimiter = new AttemptRateLimiter(MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION);

    public void assertAllowed(String inviteCode) {
        if (rateLimiter.isBlocked(inviteCode)) {
            throw new TooManyInvitationAttemptsException();
        }
    }

    public void recordFailure(String inviteCode) {
        rateLimiter.recordFailure(inviteCode);
    }

    public void recordSuccess(String inviteCode) {
        rateLimiter.reset(inviteCode);
    }
}
