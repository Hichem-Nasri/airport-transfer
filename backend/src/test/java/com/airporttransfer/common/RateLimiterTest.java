package com.airporttransfer.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RateLimiterTest {

    /** A clock the test moves by hand. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-25T10:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    MutableClock clock = new MutableClock();
    RateLimiter limiter = new RateLimiter(clock);
    Duration window = Duration.ofMinutes(15);

    @Test
    void blocksOnceTheLimitIsReachedWithin429() {
        for (int i = 0; i < 3; i++) {
            limiter.assertAllowed("k", 3, window);
            limiter.record("k", window);
        }
        assertThatThrownBy(() -> limiter.assertAllowed("k", 3, window))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> {
                assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(((ApiException) e).getCode()).isEqualTo("TOO_MANY_REQUESTS");
            });
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) {
            limiter.record("a", window);
        }
        assertThatCode(() -> limiter.assertAllowed("b", 3, window)).doesNotThrowAnyException();
    }

    @Test
    void eventsExpireWithTheWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.record("k", window);
        }
        clock.now = clock.now.plus(window).plusSeconds(1);
        assertThatCode(() -> limiter.assertAllowed("k", 3, window)).doesNotThrowAnyException();
    }

    @Test
    void clearForgetsTheKey() {
        for (int i = 0; i < 3; i++) {
            limiter.record("k", window);
        }
        limiter.clear("k");
        assertThatCode(() -> limiter.assertAllowed("k", 3, window)).doesNotThrowAnyException();
    }
}
