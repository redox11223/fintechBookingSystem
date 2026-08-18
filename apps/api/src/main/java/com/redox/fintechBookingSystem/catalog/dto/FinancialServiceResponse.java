package com.redox.fintechBookingSystem.catalog.dto;

import java.util.UUID;

public record FinancialServiceResponse(
        UUID id,
        String categoryName,
        String name,
        int durationMinutes,
        boolean isActive
) {
}
