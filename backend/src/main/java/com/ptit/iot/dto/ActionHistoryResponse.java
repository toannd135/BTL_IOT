package com.ptit.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptit.iot.entity.Action;

import java.time.LocalDateTime;

/** 3.3.5 GET /api/v1/actions/history */
public record ActionHistoryResponse(
        Integer id,
        @JsonProperty("device_id") Integer deviceId,
        @JsonProperty("device_name") String deviceName,
        @JsonProperty("user_id") Integer userId,
        String username,
        String action,
        String status,
        LocalDateTime time
) {
    public static ActionHistoryResponse from(Action a) {
        return new ActionHistoryResponse(
                a.getId(),
                a.getDevice().getId(), a.getDevice().getName(),
                a.getUser().getId(), a.getUser().getUsername(),
                a.getAction(), a.getStatus(), a.getTime());
    }
}
