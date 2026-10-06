package com.ptit.iot.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 3.3.7 Payload published to /esp8266/device-control: { device_id, action, user_id } */
public record DeviceControlPayload(
        @JsonProperty("device_id") Integer deviceId,
        String action,
        @JsonProperty("user_id") Integer userId
) {}
