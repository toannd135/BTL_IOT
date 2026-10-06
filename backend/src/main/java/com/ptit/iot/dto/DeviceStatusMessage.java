package com.ptit.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptit.iot.entity.Action;

import java.time.LocalDateTime;

/** 3.3.10 WebSocket message pushed to /topic/devices/{device_id}/status */
public record DeviceStatusMessage(
        @JsonProperty("action_id") Integer actionId,
        @JsonProperty("device_id") Integer deviceId,
        @JsonProperty("user_id") Integer userId,
        String action,
        String status,
        LocalDateTime timestamp
) {
    public static DeviceStatusMessage from(Action a) {
        return new DeviceStatusMessage(a.getId(), a.getDevice().getId(), a.getUser().getId(),
                a.getAction(), a.getStatus(), a.getTime());
    }
}
