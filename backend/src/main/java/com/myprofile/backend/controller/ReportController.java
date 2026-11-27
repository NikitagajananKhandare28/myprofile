package com.myprofile.backend.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.myprofile.backend.dto.ReportRequest;
import com.myprofile.backend.security.ClientIp;
import com.myprofile.backend.security.RateLimiter;
import com.myprofile.backend.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final RateLimiter limiter;

    public ReportController(ReportService reportService, RateLimiter limiter) {
        this.reportService = reportService;
        this.limiter = limiter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody ReportRequest request,
                                      HttpServletRequest http) {
        limiter.check("report-ip:" + ClientIp.of(http), 5, Duration.ofHours(1));
        limiter.check("report-name:" + request.username(), 30, Duration.ofHours(1));
        reportService.create(request.username(), request.reason(), request.message());
        return Map.of("status", "received");
    }
}