package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class EmailVerificationTokenTests {
  private static final Instant NOW = Instant.parse("2026-10-04T15:30:00Z");

  @Test
  void revokesOpenTokenAtRequestedInstant() {
    EmailVerificationToken token = token();

    token.revokeAt(NOW);

    assertThat(ReflectionTestUtils.getField(token, "revokedAt")).isEqualTo(NOW);
  }

  @Test
  void rejectsMissingRevocationTime() {
    assertThatNullPointerException()
        .isThrownBy(() -> token().revokeAt(null))
        .withMessage("Revocation time is required");
  }

  @Test
  void rejectsTokenThatIsAlreadyClosed() {
    EmailVerificationToken revokedToken = token();
    revokedToken.revokeAt(NOW.minusSeconds(1));
    EmailVerificationToken consumedToken = token();
    ReflectionTestUtils.setField(consumedToken, "consumedAt", NOW.minusSeconds(1));

    assertThatIllegalStateException()
        .isThrownBy(() -> revokedToken.revokeAt(NOW))
        .withMessage("Only an open token can be revoked");
    assertThatIllegalStateException()
        .isThrownBy(() -> consumedToken.revokeAt(NOW))
        .withMessage("Only an open token can be revoked");
  }

  private EmailVerificationToken token() {
    return new EmailVerificationToken(
        new User("client@example.com", "password-hash"),
        new byte[32],
        NOW.plusSeconds(3600));
  }
}
