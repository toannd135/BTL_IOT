package com.ptit.iot.service;

import com.ptit.iot.config.WebSocketConfig;
import com.ptit.iot.dto.DeviceStatusMessage;
import com.ptit.iot.dto.LatestSensorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Pushes realtime events to the frontend over STOMP (3.3.9, 3.3.10). */
@Service
public class RealtimePublisher {
    private static final Logger log = LoggerFactory.getLogger(RealtimePublisher.class);
    private final SimpMessagingTemplate template;

    public RealtimePublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void publishSensorData(LatestSensorResponse reading) {
        template.convertAndSend(WebSocketConfig.TOPIC_SENSORS, reading);
    }

    public void publishDeviceStatus(DeviceStatusMessage message) {
        String destination = String.format(WebSocketConfig.TOPIC_DEVICE_STATUS, message.deviceId());
        template.convertAndSend(destination, message);
        log.debug("[WS] {} {}", destination, message);
    }
}
