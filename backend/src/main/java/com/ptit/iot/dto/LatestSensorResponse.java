package com.ptit.iot.dto;

import java.time.LocalDateTime;

/** 3.3.1 GET /api/v1/sensors/latest */
public record LatestSensorResponse(Double temperature, Double humidity, Double light, LocalDateTime timestamp) {}
