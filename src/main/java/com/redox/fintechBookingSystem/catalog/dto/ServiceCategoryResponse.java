package com.redox.fintechBookingSystem.catalog.dto;

import java.util.UUID;

public record ServiceCategoryResponse(
        UUID id,
        String name,
        String description
) {
}
