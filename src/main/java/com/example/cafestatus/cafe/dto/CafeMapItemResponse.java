package com.example.cafestatus.cafe.dto;

import com.example.cafestatus.status.dto.StatusSummary;

import java.time.Instant;

public record CafeMapItemResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        String address,
        Instant createdAt,
        StatusSummary status
) {
}
