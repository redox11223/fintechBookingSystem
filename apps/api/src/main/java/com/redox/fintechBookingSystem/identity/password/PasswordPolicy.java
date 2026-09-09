package com.redox.fintechBookingSystem.identity.password;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

@Component
public class PasswordPolicy {
  private final IdentityProperties.Password identityPassword;
  public PasswordPolicy(IdentityProperties identityProperties){
    this.identityPassword=identityProperties.password();
  }

  public String validateAndNormalize(String rawPassword){
    if(rawPassword == null || rawPassword.isBlank()) {
      throw new InvalidPasswordException(PasswordViolation.REQUIRED);
    }
    String normalizedPassword = Normalizer.normalize(rawPassword, Normalizer.Form.NFC);
    int codePoints= normalizedPassword.codePointCount(0, normalizedPassword.length());
    int utf8Bytes = normalizedPassword.getBytes(StandardCharsets.UTF_8).length;
    if (codePoints < identityPassword.minCharacters()) {
      throw new InvalidPasswordException(PasswordViolation.TOO_SHORT);
    }
    if (codePoints > identityPassword.maxCharacters()) {
      throw new InvalidPasswordException(PasswordViolation.TOO_LONG);
    }
    if (utf8Bytes > identityPassword.maxUtf8Bytes()) {
      throw new InvalidPasswordException(PasswordViolation.TOO_MANY_UTF8_BYTES);
    }
    return normalizedPassword;
  }
}
