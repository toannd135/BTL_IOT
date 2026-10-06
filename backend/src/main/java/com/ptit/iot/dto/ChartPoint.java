package com.ptit.iot.dto;

import java.time.LocalDateTime;

/** 3.3.2 GET /api/v1/sensors/chart - one point of the time series. Fields not requested are omitted (null). */
public record ChartPoint(LocalDateTime timestamp, Double temperature, Double humidity, Double light) {}
