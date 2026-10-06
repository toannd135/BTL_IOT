package com.ptit.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** 3.3.3 POST /api/v1/devices/control request body. user_id is optional (defaults to app.default-user-id). */
public record DeviceControlRequest(
        @NotNull(message = "device_id is required") @JsonProperty("device_id") Integer deviceId,
        @NotNull(message = "action is required") String action,
        @JsonProperty("user_id") Integer userId
) {}
