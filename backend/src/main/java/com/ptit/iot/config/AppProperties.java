package com.ptit.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Integer defaultUserId,
        Device device,
        Chart chart
) {
    public record Cors(List<String> allowedOrigins) {}
    public record Device(int pendingTimeoutSeconds) {}
    public record Chart(int defaultPoints) {}
}
