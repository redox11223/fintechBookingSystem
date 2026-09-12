package com.redox.fintechBookingSystem.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.jdk.StringDeserializer;

public record ClientRegistrationRequest(
        @NotBlank(message = "The name can't be blank")
        @Size(min = 2,max = 120,message = "Name must be between 2 and 120 characters")
        String fullName,

        @NotBlank(message = "The email can't be blank")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @NotBlank(message = "The password can't be blank")
        //this overrides the global TrimmingJsonDeserializer because we don't want to strip leading
        //and trailing spaces in passwords
        @JsonDeserialize(using = StringDeserializer.class)
        String password,

        @NotBlank(message = "The phone number can't be blank")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",message = "Invalid phone number")
        String phoneNumber
) {
}
