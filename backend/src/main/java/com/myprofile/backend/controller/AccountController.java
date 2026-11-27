package com.myprofile.backend.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;
import com.myprofile.backend.exception.AppException;
import com.myprofile.backend.security.RateLimiter;
import com.myprofile.backend.service.AccountService;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private static final long MAX_AUTH_AGE_SECONDS = 600;

    private final AccountService accountService;
    private final RateLimiter limiter;

    public AccountController(AccountService accountService, RateLimiter limiter) {
        this.accountService = accountService;
        this.limiter = limiter;
    }

    @DeleteMapping
    public Map<String, String> delete(Authentication auth) {
        limiter.check("delete:" + auth.getName(), 5, Duration.ofHours(1));

        FirebaseToken token = (FirebaseToken) auth.getCredentials();
        Object authTime = token.getClaims().get("auth_time");
        long signedInAt = authTime instanceof Number n ? n.longValue() : 0L;
        if (Instant.now().getEpochSecond() - signedInAt > MAX_AUTH_AGE_SECONDS) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "REAUTH_REQUIRED",
                    "Please log in again, then delete your account");
        }
        accountService.deleteAccount(auth.getName());
        return Map.of("status", "deleted");
    }
}