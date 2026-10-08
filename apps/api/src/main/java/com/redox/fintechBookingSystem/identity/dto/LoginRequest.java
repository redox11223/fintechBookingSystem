package com.redox.fintechBookingSystem.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.jdk.StringDeserializer;

public record LoginRequest(
        @NotBlank(message = "The email can't be blank")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @NotBlank(message = "The password can't be blank")
        @JsonDeserialize(using = StringDeserializer.class)
        String password
) {
}
