package com.ptit.iot.controller;

import com.ptit.iot.common.ApiResponse;
import com.ptit.iot.common.Pagination;
import com.ptit.iot.dto.ChartPoint;
import com.ptit.iot.dto.LatestSensorResponse;
import com.ptit.iot.dto.SensorDataResponse;
import com.ptit.iot.service.SensorService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensors")
public class SensorController {
    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    /** 3.3.1 */
    @GetMapping("/latest")
    public ApiResponse<LatestSensorResponse> latest() {
        return ApiResponse.success("Successfully retrieved the latest sensor data", sensorService.latest());
    }

    /** 3.3.2 - ?type=temperature|humidity|light|{sensor_id}  &limit=20 */
    @GetMapping("/chart")
    public ApiResponse<List<ChartPoint>> chart(@RequestParam(required = false) String type,
                                               @RequestParam(required = false) Integer limit) {
        return ApiResponse.success("Successfully retrieved chart data", sensorService.chart(type, limit));
    }

    /** 3.3.4 - ?name=temperature&search=29&page=1&limit=10&sort_by=time&sort_dir=desc&from=...&to=... */
    @GetMapping("/sensor-data")
    public ApiResponse<List<SensorDataResponse>> sensorData(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "sort_by", defaultValue = "time") String sortBy,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {
        Page<SensorDataResponse> result = sensorService.sensorData(name, search, from, to, page, limit, sortBy, sortDir);
        return ApiResponse.success("Successfully retrieved list sensor data", result.getContent(), Pagination.of(result));
    }
}
