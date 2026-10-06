package com.ptit.iot.controller;

import com.ptit.iot.common.ApiResponse;
import com.ptit.iot.common.Pagination;
import com.ptit.iot.dto.ActionHistoryResponse;
import com.ptit.iot.service.ActionService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {
    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    /** 3.3.5 - ?status=success&device_id=1&action=ON&search=admin&page=1&limit=10&sort_by=time&sort_dir=desc */
    @GetMapping("/history")
    public ApiResponse<List<ActionHistoryResponse>> history(
            @RequestParam(required = false) String status,
            @RequestParam(name = "device_id", required = false) Integer deviceId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "sort_by", defaultValue = "time") String sortBy,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {
        Page<ActionHistoryResponse> result = actionService.history(status, deviceId, action, search, from, to, page, limit, sortBy, sortDir);
        return ApiResponse.success("Successfully retrieved action history", result.getContent(), Pagination.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActionHistoryResponse> get(@PathVariable Integer id) {
        return ApiResponse.success("Successfully retrieved action", actionService.get(id));
    }
}
