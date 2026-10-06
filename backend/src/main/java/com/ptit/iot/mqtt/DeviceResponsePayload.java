package com.ptit.iot.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 3.3.8 Payload on /esp8266/device-response: { device_id, user_id, action, status, time? } */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceResponsePayload(
        @JsonProperty("device_id") Integer deviceId,
        @JsonProperty("user_id") Integer userId,
        String action,
        String status,
        String time
) {}
