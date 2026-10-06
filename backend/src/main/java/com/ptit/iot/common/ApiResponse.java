package com.ptit.iot.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Response envelope used by every REST endpoint, matching the design document:
 * { "status": "success|fail|error", "code": 200, "message": "...", "pagination": {...}, "data": ..., "error_detail": "..." }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "code", "message", "pagination", "data", "error_detail"})
public record ApiResponse<T>(
        String status,
        int code,
        String message,
        Pagination pagination,
        T data,
        @JsonProperty("error_detail") String errorDetail
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", 200, message, null, data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data, Pagination pagination) {
        return new ApiResponse<>("success", 200, message, pagination, data, null);
    }

    public static ApiResponse<Void> fail(int code, String message, String errorDetail) {
        return new ApiResponse<>("fail", code, message, null, null, errorDetail);
    }

    public static ApiResponse<Void> error(int code, String message, String errorDetail) {
        return new ApiResponse<>("error", code, message, null, null, errorDetail);
    }
}
