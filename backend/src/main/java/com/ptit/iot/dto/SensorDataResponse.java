package com.ptit.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptit.iot.entity.DataSensor;

import java.time.LocalDateTime;

/** 3.3.4 GET /api/v1/sensors/sensor-data */
public record SensorDataResponse(
        Integer id,
        @JsonProperty("sensor_id") Integer sensorId,
        @JsonProperty("sensor_name") String sensorName,
        Double value,
        LocalDateTime time
) {
    public static SensorDataResponse from(DataSensor d) {
        return new SensorDataResponse(d.getId(), d.getSensor().getId(), d.getSensor().getName(), d.getValue(), d.getTime());
    }
}
