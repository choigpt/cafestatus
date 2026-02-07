package com.example.cafestatus.status.dto;

import com.example.cafestatus.status.entity.Availability;
import com.example.cafestatus.status.entity.CrowdLevel;
import jakarta.validation.constraints.NotNull;

public record UpdateCafeStatusRequest(
        @NotNull CrowdLevel crowdLevel,
        @NotNull Availability party2,
        @NotNull Availability party3,
        @NotNull Availability party4
) {}
