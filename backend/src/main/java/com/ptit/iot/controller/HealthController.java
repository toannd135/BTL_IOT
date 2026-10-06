package com.ptit.iot.controller;

import com.ptit.iot.common.ApiResponse;
import com.ptit.iot.mqtt.MqttGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final MqttGateway mqtt;

    public HealthController(MqttGateway mqtt) {
        this.mqtt = mqtt;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success("OK", Map.of("mqtt_connected", mqtt.isConnected()));
    }
}
