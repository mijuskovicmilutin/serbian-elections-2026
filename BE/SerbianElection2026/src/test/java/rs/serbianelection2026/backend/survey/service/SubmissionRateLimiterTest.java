package rs.serbianelection2026.backend.survey.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SubmissionRateLimiterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-01T10:00:00Z"));
    private final Clock clock = new Clock() {
        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };

    @Test
    void allowsUpToTheLimitPerAddressPerHour() {
        SubmissionRateLimiter limiter = new SubmissionRateLimiter(clock, 3);

        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("10.0.0.1")).isFalse();
        assertThat(limiter.tryAcquire("10.0.0.2")).isTrue(); // another address is unaffected
    }

    @Test
    void theWindowSlides() {
        SubmissionRateLimiter limiter = new SubmissionRateLimiter(clock, 1);

        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
        now.set(now.get().plus(Duration.ofMinutes(59)));
        assertThat(limiter.tryAcquire("10.0.0.1")).isFalse();
        now.set(now.get().plus(Duration.ofMinutes(2)));
        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
    }
}
