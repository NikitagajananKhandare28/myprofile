package com.myprofile.backend.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myprofile.backend.service.AdminService;

import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/reports/{id}/takedown")
    public Map<String, String> takedown(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9]{1,40}$") String id, Authentication auth) {
        adminService.takedown(id, auth.getName());
        return Map.of("status", "taken_down");
    }

    @PostMapping("/reports/{id}/dismiss")
    public Map<String, String> dismiss(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9]{1,40}$") String id, Authentication auth) {
        adminService.dismiss(id, auth.getName());
        return Map.of("status", "dismissed");
    }

    @PostMapping("/profiles/{username}/reinstate")
    public Map<String, String> reinstate(
            @PathVariable @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,29}$") String username,
            Authentication auth) {
        adminService.reinstate(username, auth.getName());
        return Map.of("status", "reinstated");
    }
}