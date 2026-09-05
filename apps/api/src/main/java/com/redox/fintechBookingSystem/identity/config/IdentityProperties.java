package com.redox.fintechBookingSystem.identity.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Non-secret identity policy. Values live in configuration so tests and deployments can override
 * operational timings without scattering constants through use cases.
 */
@Validated
@ConfigurationProperties("citafin.identity")
public record IdentityProperties(
    @Valid @NotNull Password password,
    @Valid @NotNull Tokens tokens,
    @Valid @NotNull Login login,
    @Valid @NotNull Session session,
    @Valid @NotNull Mfa mfa,
    @Valid @NotNull Mail mail,
    @NotNull List<@NotBlank String> allowedOrigins
) {
  public record Password(
      @Min(15) int minCharacters,
      @Min(64) int maxCharacters,
      @Min(72) int maxUtf8Bytes,
      @Min(10) @Max(16) int bcryptStrength
  ) {
  }

  public record Tokens(
      @NotNull Duration emailVerificationTtl,
      @NotNull Duration passwordResetTtl,
      @NotNull Duration staffInvitationTtl,
      @NotNull Duration mfaChallengeTtl,
      @NotNull Duration resendCooldown
  ) {
  }

  public record Login(
      @Min(1) int maxFailures,
      @NotNull Duration failureWindow,
      @NotNull Duration lockDuration
  ) {
  }

  public record Session(
      @NotNull Duration accessTokenTtl,
      @NotNull Duration refreshTokenTtl,
      @NotBlank String refreshCookieName,
      boolean secureCookie
  ) {
  }

  public record Mfa(
      @Min(6) @Max(8) int codeLength,
      @Min(15) int timeStepSeconds,
      @Min(0) @Max(2) int allowedTimeStepDrift,
      @Min(1) int recoveryCodeCount
  ) {
  }

  public record Mail(
      @NotBlank String from,
      @NotNull URI frontendBaseUrl
  ) {
  }
}
