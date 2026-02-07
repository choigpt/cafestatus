package com.example.cafestatus.status.dto;

import java.time.Instant;

public record StatusSummary(
        String crowdLevel,
        String party2,
        String party3,
        String party4,
        Instant updatedAt,
        Instant expiresAt,
        boolean stale,
        long ageMinutes
) {}
