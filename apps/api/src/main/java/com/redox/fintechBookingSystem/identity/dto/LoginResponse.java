package com.redox.fintechBookingSystem.identity.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
