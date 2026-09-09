package com.redox.fintechBookingSystem.identity.password;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTests {
  private final PasswordPolicy passwordPolicy = new PasswordPolicy(identityProperties());

  @Test
  void rejectsMissingPassword() {
    assertViolation(null, PasswordViolation.REQUIRED);
    assertViolation(" \t\n", PasswordViolation.REQUIRED);
  }

  @Test
  void acceptsCharacterLengthBoundaries() {
    assertThat(passwordPolicy.validateAndNormalize("a".repeat(15)))
        .isEqualTo("a".repeat(15));
    assertThat(passwordPolicy.validateAndNormalize("a".repeat(64)))
        .isEqualTo("a".repeat(64));
  }

  @Test
  void rejectsPasswordOutsideCharacterLengthBoundaries() {
    assertViolation("a".repeat(14), PasswordViolation.TOO_SHORT);
    assertViolation("a".repeat(65), PasswordViolation.TOO_LONG);
  }

  @Test
  void countsUnicodeCodePointsInsteadOfUtf16CodeUnits() {
    String fourteenCodePoints = "😀".repeat(14);

    assertViolation(fourteenCodePoints, PasswordViolation.TOO_SHORT);
  }

  @Test
  void acceptsExactlySeventyTwoUtf8Bytes() {
    String eighteenEmojis = "😀".repeat(18);

    assertThat(passwordPolicy.validateAndNormalize(eighteenEmojis))
        .isEqualTo(eighteenEmojis);
  }

  @Test
  void rejectsMoreThanSeventyTwoUtf8Bytes() {
    assertViolation("😀".repeat(19), PasswordViolation.TOO_MANY_UTF8_BYTES);
  }

  @Test
  void reportsCharacterLimitBeforeUtf8ByteLimit() {
    assertViolation("😀".repeat(65), PasswordViolation.TOO_LONG);
  }

  @Test
  void measuresCharacterLengthAfterNfcNormalization() {
    String fifteenCodePointsBeforeNormalization = "a".repeat(13) + "e\u0301";

    assertViolation(fifteenCodePointsBeforeNormalization, PasswordViolation.TOO_SHORT);
  }

  @Test
  void normalizesToNfcWithoutTrimming() {
    String decomposedPassword = "  Cafe\u0301 password 123  ";
    String expectedPassword = Normalizer.normalize(decomposedPassword, Normalizer.Form.NFC);

    assertThat(passwordPolicy.validateAndNormalize(decomposedPassword))
        .isEqualTo(expectedPassword)
        .startsWith("  ")
        .endsWith("  ");
  }

  @Test
  void exceptionDoesNotExposeRejectedPassword() {
    String rejectedPassword = "secret";

    assertThatThrownBy(() -> passwordPolicy.validateAndNormalize(rejectedPassword))
        .isInstanceOf(InvalidPasswordException.class)
        .hasMessageNotContaining(rejectedPassword);
  }

  private void assertViolation(String rawPassword, PasswordViolation expectedViolation) {
    assertThatThrownBy(() -> passwordPolicy.validateAndNormalize(rawPassword))
        .isInstanceOfSatisfying(InvalidPasswordException.class,
            exception -> assertThat(exception.getViolation()).isEqualTo(expectedViolation));
  }

  private IdentityProperties identityProperties() {
    var password = new IdentityProperties.Password(15, 64, 72, 12);
    return new IdentityProperties(password, null, null, null, null, null, List.of());
  }
}
