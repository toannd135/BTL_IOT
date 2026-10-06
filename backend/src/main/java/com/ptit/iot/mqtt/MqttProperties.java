package com.ptit.iot.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mqtt")
public record MqttProperties(
        boolean enabled,
        String brokerUrl,
        String username,
        String password,
        String clientId,
        int qos,
        Topics topics
) {
    public record Topics(String sensorData, String deviceControl, String deviceResponse) {}
}
