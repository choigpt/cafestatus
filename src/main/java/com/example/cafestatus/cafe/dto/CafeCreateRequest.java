package com.example.cafestatus.cafe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CafeCreateRequest(
        @NotBlank String name,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String address
) {}
