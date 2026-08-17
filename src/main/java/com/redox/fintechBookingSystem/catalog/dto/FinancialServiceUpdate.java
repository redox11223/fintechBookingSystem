package com.redox.fintechBookingSystem.catalog.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record FinancialServiceUpdate(
        @NotNull(message = "The category id can't be null")
        UUID categoryId,

        @NotBlank(message = "Name can't be blank")
        @Size(min = 2,max = 100,message = "Name must be between 2 and 100 characters")
        String name,

        @Positive
        @Min(value=15)
        @Max(value = 240)
        int durationMinutes,

        boolean isActive
) {
}
