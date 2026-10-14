package com.redox.fintechBookingSystem.identity.authentication;

import com.redox.fintechBookingSystem.identity.authentication.jwt.IssuedAccessToken;
import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;

import java.time.Instant;
import java.util.Objects;

public record AuthenticatedResult(
        IssuedAccessToken accessToken,
        GeneratedSecureToken refreshToken,
        long accessTokenExpiresInSeconds,
        Instant sessionExpiresAt
) {
  public AuthenticatedResult {
    Objects.requireNonNull(accessToken);
    Objects.requireNonNull(refreshToken);
    Objects.requireNonNull(sessionExpiresAt);
    if(accessTokenExpiresInSeconds<=0){
      throw new IllegalArgumentException("The expiration time has to be bigger than zero");
    }
  }
}
