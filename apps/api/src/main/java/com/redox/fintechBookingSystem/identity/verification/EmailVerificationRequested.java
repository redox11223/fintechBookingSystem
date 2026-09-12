package com.redox.fintechBookingSystem.identity.verification;

import org.springframework.util.Assert;

public record EmailVerificationRequested(
        String email,
        String rawToken
) {
  public EmailVerificationRequested{
    Assert.hasText(email,"Email can't be blank");
    Assert.hasText(rawToken,"Raw token can't be blank");
  }
  @Override
  public String toString() {
    return  "EmailVerificationRequested[email=" + email + ", rawToken=[REDACTED]]";
  }
}
