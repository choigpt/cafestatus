package com.example.cafestatus.status.cache;

import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.entity.CafeLiveStatus;

import java.time.Duration;
import java.time.Instant;

public record StatusCacheModel(
        Long cafeId,
        String crowdLevel,
        String party2,
        String party3,
        String party4,
        String updatedAt,
        String expiresAt
) {

    private static final long STALE_MINUTES = 30;

    public static StatusCacheModel from(CafeLiveStatus status) {
        return new StatusCacheModel(
                status.getCafeId(),
                status.getCrowdLevel().name(),
                status.getParty2().name(),
                status.getParty3().name(),
                status.getParty4().name(),
                status.getUpdatedAt().toString(),
                status.getExpiresAt().toString()
        );
    }

    public StatusSummary toSummary(Instant now) {
        try {
            Instant updated = Instant.parse(updatedAt);
            Instant expires = Instant.parse(expiresAt);
            long ageMinutes = Duration.between(updated, now).toMinutes();
            boolean stale = ageMinutes >= STALE_MINUTES;

            return new StatusSummary(
                    crowdLevel,
                    party2,
                    party3,
                    party4,
                    updated,
                    expires,
                    stale,
                    ageMinutes
            );
        } catch (Exception e) {
            return null;
        }
    }
}
