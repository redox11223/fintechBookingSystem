package com.redox.fintechBookingSystem.identity.verification;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class EmailVerificationTokenGenerator {
  private final SecureRandom secureRandom=new SecureRandom();//safe with concurrency by design
  private final static int TOKEN_ENTROPY_BYTES =32; //32 bytes = 256 bits
  private final static String HASH_ALGORITHM="SHA-256";

  public GeneratedEmailVerificationToken generate(){
    byte[] randomBytes=new byte[TOKEN_ENTROPY_BYTES];
    secureRandom.nextBytes(randomBytes);
    String rawToken=Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    byte[] tokenHash=hash(rawToken);
    return new GeneratedEmailVerificationToken(rawToken,tokenHash);
  }
  //this method is used to hash the token before storing it in the database for security reasons
  // and also to compare the token provided by the user with the one stored in the database.
  public byte[] hash(String rawToken){
    Objects.requireNonNull(rawToken,"Token must not be null");
    byte[] utf8Bytes=rawToken.getBytes(StandardCharsets.UTF_8);
    return getSha256Digest().digest(utf8Bytes); //SHA-256 algorithm produces a 32-byte hash
  }

  private MessageDigest getSha256Digest(){
    try {
      return MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is unavailable",e);
    }
  }
}
