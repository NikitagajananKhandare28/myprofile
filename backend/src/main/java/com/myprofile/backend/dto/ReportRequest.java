package com.myprofile.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,29}$") String username,
        @NotBlank @Size(max = 60) String reason,
        @Size(max = 300) String message) {
}