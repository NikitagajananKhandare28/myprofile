package com.myprofile.backend.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.myprofile.backend.exception.AppException;

class RateLimiterTest {

    private final Duration minute = Duration.ofMinutes(1);

    @Test
    void allowsUpToTheLimitThenBlocks() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(now::get);
        for (int i = 0; i < 3; i++) {
            limiter.check("k", 3, minute);
        }
        AppException e = assertThrows(AppException.class, () -> limiter.check("k", 3, minute));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
    }

    @Test
    void windowSlides() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(now::get);
        limiter.check("k", 1, minute);
        assertThrows(AppException.class, () -> limiter.check("k", 1, minute));
        now.set(61_000);
        assertDoesNotThrow(() -> limiter.check("k", 1, minute));
    }

    @Test
    void keysAreIndependent() {
        RateLimiter limiter = new RateLimiter(() -> 0L);
        limiter.check("a", 1, minute);
        assertDoesNotThrow(() -> limiter.check("b", 1, minute));
    }
}