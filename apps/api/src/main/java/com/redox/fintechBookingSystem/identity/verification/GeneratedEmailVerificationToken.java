package com.redox.fintechBookingSystem.identity.verification;

import java.util.Arrays;
import java.util.Objects;

public record GeneratedEmailVerificationToken(
        String rawToken,
        byte[] tokenHash
) {
  private final static int SHA256_HASH_LENGTH=32; //SHA-256 produces a 32-byte hash

  public GeneratedEmailVerificationToken{
    if(rawToken==null || rawToken.isBlank()){
      throw new IllegalArgumentException("Raw token must not be null or blank");
    }
    Objects.requireNonNull(tokenHash,"Token hash must not be null");
    if(tokenHash.length!= SHA256_HASH_LENGTH){
      throw new IllegalArgumentException("Token hash must be 32 bytes");
    }
    tokenHash = Arrays.copyOf(tokenHash, tokenHash.length);
  }
  @Override
  public byte[] tokenHash() {
    return Arrays.copyOf(tokenHash, tokenHash.length);
  }
}
