package com.redox.fintechBookingSystem.identity.authentication.jwt;

import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;

public record IssuedAccessToken(
        String value,
        Instant expiresAt
) {
  public IssuedAccessToken{
    Assert.hasText(value,"Access Token is required");
    Objects.requireNonNull(expiresAt,"Access Token expiration is required");
  }
  @Override
  public String toString() {
    return "IssuedAccessToken[value=[REDACTED], expiresAt="
            + expiresAt + "]";
  }

}
