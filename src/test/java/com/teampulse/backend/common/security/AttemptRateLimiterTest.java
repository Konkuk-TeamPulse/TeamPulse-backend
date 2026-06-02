package com.teampulse.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AttemptRateLimiterTest {

    @Test
    void blocksAfterConfiguredFailuresAndAllowsReset() {
        var clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
        var limiter = new AttemptRateLimiter(3, Duration.ofMinutes(10), clock);

        limiter.recordFailure("USER@example.com");
        limiter.recordFailure("user@example.com");

        assertThat(limiter.isBlocked(" user@example.com ")).isFalse();

        limiter.recordFailure("user@example.com");
        assertThat(limiter.isBlocked("USER@example.com")).isTrue();

        limiter.reset("user@example.com");
        assertThat(limiter.isBlocked("user@example.com")).isFalse();
    }
}
