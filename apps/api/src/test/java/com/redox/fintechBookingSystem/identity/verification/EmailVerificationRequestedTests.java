package com.redox.fintechBookingSystem.identity.verification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class EmailVerificationRequestedTests {
  private static final String RAW_TOKEN = "private-raw-token";

  @Test
  void retainsDeliveryDataWithoutExposingTokenInTextRepresentation() {
    EmailVerificationRequested event =
        new EmailVerificationRequested("client@example.com", RAW_TOKEN);

    assertThat(event.email()).isEqualTo("client@example.com");
    assertThat(event.rawToken()).isEqualTo(RAW_TOKEN);
    assertThat(event.toString())
        .contains("client@example.com", "rawToken=[REDACTED]")
        .doesNotContain(RAW_TOKEN);
  }

  @Test
  void rejectsMissingDeliveryData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new EmailVerificationRequested(" ", RAW_TOKEN));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new EmailVerificationRequested("client@example.com", "\t"));
  }
}
