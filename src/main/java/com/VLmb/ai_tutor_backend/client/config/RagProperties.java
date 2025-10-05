package com.VLmb.ai_tutor_backend.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "clients.rag")
public record RagProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration responseTimeout,
        int maxMemorySizeMegabytes,
        boolean logPayloads
) {
}
