package com.example.cafestatus.status.dto;

public record CafeStatusSseEvent(
        Long cafeId,
        StatusSummary status
) {}
