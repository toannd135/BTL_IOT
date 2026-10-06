package com.ptit.iot.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

public record Pagination(
        int page,
        int limit,
        @JsonProperty("total_items") long totalItems,
        @JsonProperty("total_pages") int totalPages
) {
    public static Pagination of(Page<?> page) {
        return new Pagination(page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
