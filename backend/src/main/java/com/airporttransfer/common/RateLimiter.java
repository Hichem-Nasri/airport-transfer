package com.airporttransfer.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A small in-memory sliding-window limiter for the endpoints that can be abused anonymously (login attempts, reset
 * e-mails). It is per application instance: behind several instances each one counts on its own, which is fine for a
 * brake on guessing and mail spam (a shared store such as Redis would be the next step if the app is scaled out).
 */
@Component
public class RateLimiter {

    private static final int PRUNE_THRESHOLD = 10_000;

    private final Clock clock;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    @Autowired
    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Throws 429 when {@code key} already has {@code max} recorded events inside the window. */
    public void assertAllowed(String key, int max, Duration window) {
        Deque<Instant> events = hits.get(key);
        if (events == null) {
            return;
        }
        synchronized (events) {
            drop(events, window);
            if (events.size() >= max) {
                throw ApiException.tooManyRequests("Too many attempts, try again later");
            }
        }
    }

    /** Counts one event for {@code key}. */
    public void record(String key, Duration window) {
        if (hits.size() > PRUNE_THRESHOLD) {
            hits.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    drop(e.getValue(), window);
                    return e.getValue().isEmpty();
                }
            });
        }
        Deque<Instant> events = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (events) {
            events.addLast(clock.instant());
        }
    }

    /** Forgets a key (for example after a successful login). */
    public void clear(String key) {
        hits.remove(key);
    }

    private void drop(Deque<Instant> events, Duration window) {
        Instant limit = clock.instant().minus(window);
        while (!events.isEmpty() && !events.peekFirst().isAfter(limit)) {
            events.removeFirst();
        }
    }
}
