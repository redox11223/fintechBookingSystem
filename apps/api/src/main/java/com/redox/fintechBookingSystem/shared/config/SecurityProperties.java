package com.redox.fintechBookingSystem.shared.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("citafin.security")
public record SecurityProperties(
    @NotEmpty List<@NotBlank String> allowedOrigins
) {
}
