package com.teampulse.backend.auth.application;

import com.teampulse.backend.common.security.AttemptRateLimiter;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptGuard {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(10);

    private final AttemptRateLimiter rateLimiter = new AttemptRateLimiter(MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION);

    public void assertAllowed(String email) {
        if (rateLimiter.isBlocked(email)) {
            throw new TooManyLoginAttemptsException();
        }
    }

    public void recordFailure(String email) {
        rateLimiter.recordFailure(email);
    }

    public void recordSuccess(String email) {
        rateLimiter.reset(email);
    }
}
