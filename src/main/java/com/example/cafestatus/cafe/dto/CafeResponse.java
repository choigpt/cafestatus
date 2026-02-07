package com.example.cafestatus.cafe.dto;

import com.example.cafestatus.cafe.entity.Cafe;

import java.time.Instant;

public record CafeResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        String address,
        Instant createdAt
) {
    public static CafeResponse from(Cafe cafe) {
        return new CafeResponse(
                cafe.getId(),
                cafe.getName(),
                cafe.getLatitude(),
                cafe.getLongitude(),
                cafe.getAddress(),
                cafe.getCreatedAt()
        );
    }
}
