package com.ptit.iot.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** 3.3.6 Payload on /esp8266/sensors-data: { "data": [ { temperature, humidity, light, timestamp } ] } */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SensorDataPayload(List<Reading> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reading(Double temperature, Double humidity, Double light, String timestamp) {}
}
