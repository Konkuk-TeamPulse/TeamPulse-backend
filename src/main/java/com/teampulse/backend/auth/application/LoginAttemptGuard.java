package com.teampulse.backend.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptGuard {

    private final ConcurrentHashMap<String, LoginAttemptState> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxFailures;
    private final int lockoutMinutes;
    private final int maxTrackedEmails;

    @Autowired
    public LoginAttemptGuard(
            @Value("${app.security.login.max-failures:5}") int maxFailures,
            @Value("${app.security.login.lockout-minutes:15}") int lockoutMinutes,
            @Value("${app.security.login.max-tracked-emails:10000}") int maxTrackedEmails
    ) {
        this(Clock.systemUTC(), maxFailures, lockoutMinutes, maxTrackedEmails);
    }

    LoginAttemptGuard(Clock clock, int maxFailures, int lockoutMinutes, int maxTrackedEmails) {
        this.clock = clock;
        this.maxFailures = Math.max(1, maxFailures);
        this.lockoutMinutes = Math.max(1, lockoutMinutes);
        this.maxTrackedEmails = Math.max(100, maxTrackedEmails);
    }

    public void assertAllowed(String email) {
        var state = attempts.get(email);
        if (state == null) {
            return;
        }
        if (state.lockedUntil() != null && state.lockedUntil().isAfter(now())) {
            throw new TooManyLoginAttemptsException();
        }
        if (state.isExpired(now(), lockoutMinutes)) {
            attempts.remove(email, state);
        }
    }

    public void recordFailure(String email) {
        cleanupExpiredAttempts();
        attempts.compute(email, (key, current) -> {
            var failures = current == null ? 1 : current.failures() + 1;
            var lockedUntil = failures >= maxFailures
                    ? now().plus(lockoutMinutes, ChronoUnit.MINUTES)
                    : null;
            return new LoginAttemptState(failures, lockedUntil, now());
        });
        enforceMaxTrackedEmails();
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    private Instant now() {
        return clock.instant();
    }

    private void cleanupExpiredAttempts() {
        var current = now();
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(current, lockoutMinutes));
    }

    private void enforceMaxTrackedEmails() {
        var overflow = attempts.size() - maxTrackedEmails;
        if (overflow <= 0) {
            return;
        }
        attempts.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().lastUpdatedAt()))
                .limit(overflow)
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(attempts::remove);
    }

    private record LoginAttemptState(int failures, Instant lockedUntil, Instant lastUpdatedAt) {
        private boolean isExpired(Instant now, int lockoutMinutes) {
            if (lockedUntil != null) {
                return !lockedUntil.isAfter(now);
            }
            return lastUpdatedAt.plus(lockoutMinutes, ChronoUnit.MINUTES).isBefore(now);
        }
    }
}
