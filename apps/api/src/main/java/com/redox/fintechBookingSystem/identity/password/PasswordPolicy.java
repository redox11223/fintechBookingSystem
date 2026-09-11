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
    // Normalize the password to NFC form to ensure consistent representation of Unicode characters
    // becuase some Unicode characters can be represented in multiple ways.For example,the character
    // "é" can be represented as a single code point (U+00E9) or as a combination of two code
    // points (U+0065 U+0301). Normalizing to NFC ensures that all equivalent representations are
    // treated the same way.
    String normalizedPassword = Normalizer.normalize(rawPassword, Normalizer.Form.NFC);
    // codePoints counts the number of Unicode code points in the normalized password, which is
    // important for accurately enforcing password length requirements, especially when dealing with
    // characters outside the Basic Multilingual Plane (BMP) that may be represented by surrogate pairs in UTF-16.
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
