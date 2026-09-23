package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.identity.verification.EmailVerificationConfirmationService;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenGenerator;
import com.redox.fintechBookingSystem.identity.verification.InvalidEmailVerificationTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
    TestcontainersConfiguration.class,
    EmailVerificationConfirmationIntegrationTests.FixedClockConfiguration.class
})
@SpringBootTest
@ActiveProfiles("test")
class EmailVerificationConfirmationIntegrationTests {
  private static final Instant NOW = Instant.parse("2026-09-22T15:30:00Z");

  @Autowired private EmailVerificationConfirmationService confirmationService;
  @Autowired private EmailVerificationTokenGenerator tokenGenerator;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void validTokenIsConsumedAndUserIsVerifiedAtTheSameInstant() {
    UUID userId = insertUser();
    String rawToken = "valid-" + UUID.randomUUID();
    UUID tokenId = insertToken(
        userId, rawToken, NOW.minusSeconds(60), NOW.plusSeconds(3600), null, null);

    confirmationService.confirm(rawToken);

    assertThat(readInstant("users", "email_verified_at", userId)).isEqualTo(NOW);
    assertThat(readInstant("email_verification_tokens", "consumed_at", tokenId)).isEqualTo(NOW);
  }

  @Test
  void unknownExpiredRevokedAndConsumedTokensHaveTheSameOutcome() {
    UUID expiredUserId = insertUser();
    String expiredToken = "expired-" + UUID.randomUUID();
    insertToken(
        expiredUserId,
        expiredToken,
        NOW.minusSeconds(7200),
        NOW.minusSeconds(3600),
        null,
        null);

    UUID revokedUserId = insertUser();
    String revokedToken = "revoked-" + UUID.randomUUID();
    insertToken(
        revokedUserId,
        revokedToken,
        NOW.minusSeconds(3600),
        NOW.plusSeconds(3600),
        null,
        NOW.minusSeconds(60));

    UUID consumedUserId = insertUser();
    String consumedToken = "consumed-" + UUID.randomUUID();
    insertToken(
        consumedUserId,
        consumedToken,
        NOW.minusSeconds(3600),
        NOW.plusSeconds(3600),
        NOW.minusSeconds(60),
        null);

    List<String> invalidTokens = List.of(
        "unknown-" + UUID.randomUUID(), expiredToken, revokedToken, consumedToken);

    for (String invalidToken : invalidTokens) {
      assertThatThrownBy(() -> confirmationService.confirm(invalidToken))
          .isInstanceOf(InvalidEmailVerificationTokenException.class)
          .hasMessage("Email verification token is invalid");
    }

    assertThat(readInstant("users", "email_verified_at", expiredUserId)).isNull();
    assertThat(readInstant("users", "email_verified_at", revokedUserId)).isNull();
    assertThat(readInstant("users", "email_verified_at", consumedUserId)).isNull();
  }

  @Test
  void concurrentConfirmationAllowsExactlyOneConsumer() throws Exception {
    UUID userId = insertUser();
    String rawToken = "concurrent-" + UUID.randomUUID();
    UUID tokenId = insertToken(
        userId, rawToken, NOW.minusSeconds(60), NOW.plusSeconds(3600), null, null);
    CyclicBarrier bothRequestsReady = new CyclicBarrier(2);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<Boolean> first = executor.submit(() -> confirmAfterBarrier(rawToken, bothRequestsReady));
      Future<Boolean> second = executor.submit(() -> confirmAfterBarrier(rawToken, bothRequestsReady));

      assertThat(List.of(
          first.get(15, TimeUnit.SECONDS),
          second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(readInstant("users", "email_verified_at", userId)).isEqualTo(NOW);
    assertThat(readInstant("email_verification_tokens", "consumed_at", tokenId)).isEqualTo(NOW);
  }

  private boolean confirmAfterBarrier(String rawToken, CyclicBarrier barrier) throws Exception {
    barrier.await(10, TimeUnit.SECONDS);
    try {
      confirmationService.confirm(rawToken);
      return true;
    } catch (InvalidEmailVerificationTokenException ex) {
      return false;
    }
  }

  private UUID insertUser() {
    UUID userId = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO users (id, email, password_hash)
        VALUES (?, ?, ?)
        """, userId, "client-%s@example.com".formatted(userId), "test-password-hash");
    return userId;
  }

  private UUID insertToken(
      UUID userId,
      String rawToken,
      Instant createdAt,
      Instant expiresAt,
      Instant consumedAt,
      Instant revokedAt
  ) {
    UUID tokenId = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO email_verification_tokens (
            id, user_id, token_hash, expires_at, consumed_at, revoked_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        tokenId,
        userId,
        tokenGenerator.hash(rawToken),
        atUtc(expiresAt),
        consumedAt == null ? null : atUtc(consumedAt),
        revokedAt == null ? null : atUtc(revokedAt),
        atUtc(createdAt),
        atUtc(latest(createdAt, consumedAt, revokedAt)));
    return tokenId;
  }

  private Instant readInstant(String table, String column, UUID id) {
    Timestamp value = jdbc.queryForObject(
        "SELECT %s FROM %s WHERE id = ?".formatted(column, table),
        Timestamp.class,
        id);
    return value == null ? null : value.toInstant();
  }

  private OffsetDateTime atUtc(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private Instant latest(Instant first, Instant second, Instant third) {
    Instant latest = first;
    if (second != null && second.isAfter(latest)) {
      latest = second;
    }
    if (third != null && third.isAfter(latest)) {
      latest = third;
    }
    return latest;
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
