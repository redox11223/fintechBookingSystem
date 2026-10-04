package com.redox.fintechBookingSystem.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ConfirmVerificationEmailRequest(
        @NotBlank(message = "The token can't be blank")
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String token
) {
}
