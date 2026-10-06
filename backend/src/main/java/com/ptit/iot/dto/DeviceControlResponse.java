package com.ptit.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** 3.3.3 POST /api/v1/devices/control response data. */
public record DeviceControlResponse(
        @JsonProperty("device_id") Integer deviceId,
        String state,
        LocalDateTime timestamp,
        @JsonProperty("action_id") Integer actionId,
        String status
) {}
