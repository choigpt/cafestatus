package com.example.cafestatus.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}
