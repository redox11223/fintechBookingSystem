package com.redox.fintechBookingSystem.identity.dto;

import org.springframework.util.Assert;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
  public LoginResponse{
    Assert.hasText(accessToken,"Access Token cant be blank");
    Assert.hasText(tokenType,"The access token type cant be blank");
    if(expiresIn<=0){
      throw new IllegalArgumentException("The expiration time has to be bigger than zero");
    }
  }
  @Override
  public String toString() {
    return "LoginResponse[accessToken=[REDACTED], tokenType="
            + tokenType
            + ", expiresIn="
            + expiresIn
            + "]";
  }

}
