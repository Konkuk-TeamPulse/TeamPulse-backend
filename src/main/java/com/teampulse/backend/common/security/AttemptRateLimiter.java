package com.teampulse.backend.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttemptRateLimiter {

    private static final int MAX_TRACKED_KEYS = 2048;

    private final int maxFailures;
    private final Duration lockoutDuration;
    private final Clock clock;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public AttemptRateLimiter(int maxFailures, Duration lockoutDuration) {
        this(maxFailures, lockoutDuration, Clock.systemUTC());
    }

    AttemptRateLimiter(int maxFailures, Duration lockoutDuration, Clock clock) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be positive.");
        }
        if (lockoutDuration == null || lockoutDuration.isNegative() || lockoutDuration.isZero()) {
            throw new IllegalArgumentException("lockoutDuration must be positive.");
        }
        this.maxFailures = maxFailures;
        this.lockoutDuration = lockoutDuration;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        var normalizedKey = normalizeKey(key);
        if (normalizedKey.isBlank()) {
            return false;
        }
        var now = Instant.now(clock);
        prune(now);
        var state = attempts.get(normalizedKey);
        return state != null && state.isBlocked(now);
    }

    public void recordFailure(String key) {
        var normalizedKey = normalizeKey(key);
        if (normalizedKey.isBlank()) {
            return;
        }
        var now = Instant.now(clock);
        prune(now);
        attempts.compute(normalizedKey, (ignored, current) -> {
            if (current != null && current.isBlocked(now)) {
                return current.withLastAttempt(now);
            }
            var failures = current == null ? 1 : current.failures() + 1;
            var lockedUntil = failures >= maxFailures ? now.plus(lockoutDuration) : null;
            return new AttemptState(failures, lockedUntil, now);
        });
    }

    public void reset(String key) {
        var normalizedKey = normalizeKey(key);
        if (!normalizedKey.isBlank()) {
            attempts.remove(normalizedKey);
        }
    }

    private void prune(Instant now) {
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now, lockoutDuration));
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        var overflow = attempts.size() - MAX_TRACKED_KEYS;
        attempts.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().lastAttempt()))
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(attempts::remove);
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private record AttemptState(int failures, Instant lockedUntil, Instant lastAttempt) {

        boolean isBlocked(Instant now) {
            return lockedUntil != null && lockedUntil.isAfter(now);
        }

        boolean isExpired(Instant now, Duration retention) {
            return !isBlocked(now) && lastAttempt.plus(retention).isBefore(now);
        }

        AttemptState withLastAttempt(Instant now) {
            return new AttemptState(failures, lockedUntil, now);
        }
    }
}
