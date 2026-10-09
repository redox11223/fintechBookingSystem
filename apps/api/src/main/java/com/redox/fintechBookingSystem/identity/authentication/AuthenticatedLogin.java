package com.redox.fintechBookingSystem.identity.authentication;

import com.redox.fintechBookingSystem.identity.authentication.jwt.IssuedAccessToken;
import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;

import java.util.Objects;

public record AuthenticatedLogin(
        IssuedAccessToken accessToken,
        GeneratedSecureToken refreshToken,
        long accessTokenExpiresInSeconds
) {
  public AuthenticatedLogin{
    Objects.requireNonNull(accessToken);
    Objects.requireNonNull(refreshToken);
    if(accessTokenExpiresInSeconds<=0){
      throw new IllegalArgumentException("The expiration time has to be bigger than zero");
    }
  }
}
