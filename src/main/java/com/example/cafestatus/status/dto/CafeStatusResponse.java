package com.example.cafestatus.status.dto;

import com.example.cafestatus.status.entity.Availability;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import com.example.cafestatus.status.entity.CrowdLevel;

import java.time.Instant;

public record CafeStatusResponse(
        Long cafeId,
        CrowdLevel crowdLevel,
        Availability party2,
        Availability party3,
        Availability party4,
        Instant updatedAt,
        Instant expiresAt,
        boolean expired
) {
    public static CafeStatusResponse from(CafeLiveStatus s, Instant now) {
        return new CafeStatusResponse(
                s.getCafeId(),
                s.getCrowdLevel(),
                s.getParty2(),
                s.getParty3(),
                s.getParty4(),
                s.getUpdatedAt(),
                s.getExpiresAt(),
                s.isExpired(now)
        );
    }
}
