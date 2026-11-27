package com.myprofile.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "myprofile")
public record AppProperties(Firebase firebase, Cors cors, String publicBaseUrl) {

    public record Firebase(String projectId, String storageBucket) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}