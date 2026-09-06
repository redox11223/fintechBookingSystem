package com.redox.fintechBookingSystem.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmVerificationEmailRequest(
        @NotBlank(message = "The token can't be blank")
        String token
) {
}
