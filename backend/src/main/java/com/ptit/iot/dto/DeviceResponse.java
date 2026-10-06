package com.ptit.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * GET /api/v1/devices - device list with its current state derived from the last successful action.
 * {@code last_status} reflects the most recent action regardless of outcome, so it can be PENDING
 * while a command is still in flight (and FAILED if it timed out / was rejected by the device).
 */
public record DeviceResponse(
        Integer id,
        String name,
        String state,
        @JsonProperty("last_action_id") Integer lastActionId,
        @JsonProperty("last_status") String lastStatus,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {}
