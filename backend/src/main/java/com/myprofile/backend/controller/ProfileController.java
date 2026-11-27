package com.myprofile.backend.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myprofile.backend.dto.PublishRequest;
import com.myprofile.backend.dto.PublishResponse;
import com.myprofile.backend.security.RateLimiter;
import com.myprofile.backend.service.PublishService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final PublishService publishService;
    private final RateLimiter limiter;

    public ProfileController(PublishService publishService, RateLimiter limiter) {
        this.publishService = publishService;
        this.limiter = limiter;
    }

    @PostMapping("/publish")
    public PublishResponse publish(@Valid @RequestBody PublishRequest request, Authentication auth) {
        limiter.check("publish:" + auth.getName(), 60, Duration.ofHours(1));
        return publishService.publish(auth.getName(), request.username());
    }

    @PostMapping("/unpublish")
    public Map<String, String> unpublish(Authentication auth) {
        limiter.check("publish:" + auth.getName(), 60, Duration.ofHours(1));
        publishService.unpublish(auth.getName());
        return Map.of("status", "unpublished");
    }
}