package com.redox.fintechBookingSystem.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceCategoryRequest(
        @NotBlank(message = "Name can't be empty")
        @Size(min = 2,max = 100,message = "Name must be between 2 and 100 characters")
        String name,
        @Size(max = 500,message = "Description must not exceed 500 characters")
        String description
) {}
