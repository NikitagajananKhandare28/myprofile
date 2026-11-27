package com.myprofile.backend.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.myprofile.backend.exception.AppException;

/**
 * Sliding-window limiter kept in memory. It protects one running instance.
 * With several instances each keeps its own counts, so treat it as basic
 * abuse protection and add Cloud Armor or a shared store for production scale.
 */
@Component
public class RateLimiter {

    private static final int MAX_KEYS = 50_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public RateLimiter() {
        this(System::currentTimeMillis);
    }

    public RateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    public void check(String key, int max, Duration window) {
        if (hits.size() > MAX_KEYS) {
            hits.clear();
        }
        long now = clock.getAsLong();
        long cutoff = now - window.toMillis();
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() <= cutoff) {
                queue.pollFirst();
            }
            if (queue.size() >= max) {
                throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                        "Too many requests. Please try again later.");
            }
            queue.addLast(now);
        }
    }
}