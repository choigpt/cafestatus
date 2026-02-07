package com.example.cafestatus.status.mapper;

import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.entity.CafeLiveStatus;

import java.time.Duration;
import java.time.Instant;

public final class StatusViewMapper {

    private static final long STALE_MINUTES = 30;

    private StatusViewMapper() {}

    public static StatusSummary unknown() {
        return new StatusSummary(
                "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN",
                null, null,
                true,
                -1
        );
    }

    public static StatusSummary from(CafeLiveStatus s, Instant now) {
        long ageMinutes = Duration.between(s.getUpdatedAt(), now).toMinutes();
        boolean stale = ageMinutes >= STALE_MINUTES;

        return new StatusSummary(
                s.getCrowdLevel().name(),
                s.getParty2().name(),
                s.getParty3().name(),
                s.getParty4().name(),
                s.getUpdatedAt(),
                s.getExpiresAt(),
                stale,
                ageMinutes
        );
    }
}
