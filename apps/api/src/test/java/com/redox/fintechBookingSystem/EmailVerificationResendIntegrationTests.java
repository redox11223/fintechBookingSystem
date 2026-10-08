package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.verification.EmailResendService;
import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "management.health.mail.enabled=false")
@ActiveProfiles("test")
class EmailVerificationResendIntegrationTests {

  @Autowired private EmailResendService resendService;
  @Autowired private SecureTokenGenerator tokenGenerator;
  @Autowired private IdentityProperties identityProperties;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private JavaMailSender mailSender;

  @Test
  void replacesOpenTokenAfterCooldownAndSendsOneEmail() {
    String email = uniqueEmail("replace");
    UUID userId = insertUser(email, null);
    String previousRawToken = "previous-" + UUID.randomUUID();
    UUID previousTokenId = insertOpenToken(
        userId,
        previousRawToken,
        Instant.now().minus(identityProperties.tokens().resendCooldown()).minusSeconds(1));

    Instant before = Instant.now();
    resendService.resend("  " + email.toUpperCase() + "  ");
    Instant after = Instant.now();

    assertThat(readInstant("email_verification_tokens", "revoked_at", previousTokenId))
        .isBetween(before, after);
    assertThat(countOpenTokens(userId)).isEqualTo(1);
    assertThat(countAllTokens(userId)).isEqualTo(2);
    Instant newExpiration = jdbc.queryForObject("""
        SELECT expires_at
        FROM email_verification_tokens
        WHERE user_id = ? AND consumed_at IS NULL AND revoked_at IS NULL
        """, Timestamp.class, userId).toInstant();
    Duration ttl = identityProperties.tokens().emailVerificationTtl();
    assertThat(newExpiration).isBetween(before.plus(ttl), after.plus(ttl));
    verify(mailSender).send(any(SimpleMailMessage.class));
  }

  @Test
  void requestInsideCooldownKeepsCurrentTokenAndSendsNoEmail() {
    String email = uniqueEmail("cooldown");
    UUID userId = insertUser(email, null);
    UUID tokenId = insertOpenToken(userId, "current-" + UUID.randomUUID(), Instant.now());

    resendService.resend(email);

    assertThat(countOpenTokens(userId)).isEqualTo(1);
    assertThat(countAllTokens(userId)).isEqualTo(1);
    assertThat(readInstant("email_verification_tokens", "revoked_at", tokenId)).isNull();
    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  void unknownAndVerifiedAccountsProduceNoObservableWork() {
    String verifiedEmail = uniqueEmail("verified");
    UUID verifiedUserId = insertUser(verifiedEmail, Instant.now().minusSeconds(1));

    resendService.resend(uniqueEmail("missing"));
    resendService.resend(verifiedEmail);

    assertThat(countAllTokens(verifiedUserId)).isZero();
    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  void concurrentRequestsCreateOnlyOneReplacementTokenAndOneEmail() throws Exception {
    String email = uniqueEmail("concurrent");
    UUID userId = insertUser(email, null);
    UUID previousTokenId = insertOpenToken(
        userId,
        "previous-" + UUID.randomUUID(),
        Instant.now().minus(identityProperties.tokens().resendCooldown()).minusSeconds(1));
    CyclicBarrier bothRequestsReady = new CyclicBarrier(2);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> first = executor.submit(() -> resendAfterBarrier(email, bothRequestsReady));
      Future<?> second = executor.submit(() -> resendAfterBarrier(email, bothRequestsReady));

      first.get(15, TimeUnit.SECONDS);
      second.get(15, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(readInstant("email_verification_tokens", "revoked_at", previousTokenId)).isNotNull();
    assertThat(countOpenTokens(userId)).isEqualTo(1);
    assertThat(countAllTokens(userId)).isEqualTo(2);
    verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
  }

  private void resendAfterBarrier(String email, CyclicBarrier barrier) {
    try {
      barrier.await(10, TimeUnit.SECONDS);
      resendService.resend(email);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private UUID insertUser(String email, Instant verifiedAt) {
    UUID userId = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO users (id, email, password_hash, email_verified_at)
        VALUES (?, ?, ?, ?)
        """,
        userId,
        email,
        "test-password-hash",
        verifiedAt == null ? null : atUtc(verifiedAt));
    return userId;
  }

  private UUID insertOpenToken(UUID userId, String rawToken, Instant createdAt) {
    UUID tokenId = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO email_verification_tokens (
            id, user_id, token_hash, expires_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?)
        """,
        tokenId,
        userId,
        tokenGenerator.hash(rawToken),
        atUtc(createdAt.plus(Duration.ofHours(24))),
        atUtc(createdAt),
        atUtc(createdAt));
    return tokenId;
  }

  private long countOpenTokens(UUID userId) {
    Long count = jdbc.queryForObject("""
        SELECT count(*)
        FROM email_verification_tokens
        WHERE user_id = ? AND consumed_at IS NULL AND revoked_at IS NULL
        """, Long.class, userId);
    return count == null ? 0 : count;
  }

  private long countAllTokens(UUID userId) {
    Long count = jdbc.queryForObject(
        "SELECT count(*) FROM email_verification_tokens WHERE user_id = ?",
        Long.class,
        userId);
    return count == null ? 0 : count;
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

  private String uniqueEmail(String prefix) {
    return "%s-%s@example.com".formatted(prefix, UUID.randomUUID());
  }
}
