package com.myprofile.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PublishRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,29}$") String username) {
}