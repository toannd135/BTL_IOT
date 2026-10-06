package com.ptit.iot.controller;

import com.ptit.iot.common.ApiResponse;
import com.ptit.iot.dto.DeviceControlRequest;
import com.ptit.iot.dto.DeviceControlResponse;
import com.ptit.iot.dto.DeviceResponse;
import com.ptit.iot.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** List devices with their current ON/OFF state (used to initialise the Device Control panel). */
    @GetMapping
    public ApiResponse<List<DeviceResponse>> list() {
        return ApiResponse.success("Successfully retrieved devices", deviceService.listDevices());
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceResponse> get(@PathVariable Integer id) {
        return ApiResponse.success("Successfully retrieved device", deviceService.getDevice(id));
    }

    /** 3.3.3 */
    @PostMapping("/control")
    public ApiResponse<DeviceControlResponse> control(@Valid @RequestBody DeviceControlRequest request) {
        return ApiResponse.success("Device control command sent successfully", deviceService.control(request));
    }
}
