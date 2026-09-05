package com.redox.fintechBookingSystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class EmailVerificationTokenMigrationIntegrationTests {

  private static final SecureRandom RANDOM = new SecureRandom();

  @Autowired
  private JdbcTemplate jdbc;

  @Test
  void acceptsValidTokenAndAppliesAuditDefaults() {
    UUID userId = insertUser();
    UUID tokenId = UUID.randomUUID();

    jdbc.update("""
        INSERT INTO email_verification_tokens (id, user_id, token_hash, expires_at)
        VALUES (?, ?, ?, ?)
        """, tokenId, userId, tokenHash(), OffsetDateTime.now().plusHours(24));

    var auditValues = jdbc.queryForMap("""
        SELECT created_at, updated_at
        FROM email_verification_tokens
        WHERE id = ?
        """, tokenId);

    assertThat(auditValues.get("created_at")).isNotNull();
    assertThat(auditValues.get("updated_at")).isNotNull();
  }

  @Test
  void rejectsInvalidOrDuplicateTokenHash() {
    UUID firstUserId = insertUser();
    UUID secondUserId = insertUser();
    byte[] hash = tokenHash();

    insertOpenToken(firstUserId, hash, OffsetDateTime.now(), OffsetDateTime.now().plusHours(24));

    assertThatThrownBy(() -> insertOpenToken(
        secondUserId, hash, OffsetDateTime.now(), OffsetDateTime.now().plusHours(24)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uq_email_verification_tokens_token_hash");

    assertThatThrownBy(() -> insertOpenToken(
        secondUserId, new byte[31], OffsetDateTime.now(), OffsetDateTime.now().plusHours(24)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("chk_email_verification_tokens_hash_length");
  }

  @Test
  void rejectsInvalidLifecycleTimestamps() {
    UUID userId = insertUser();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-05T10:00:00-05:00");
    OffsetDateTime expiresAt = createdAt.plusHours(24);

    assertTokenRejected(userId, createdAt, null, null, null, createdAt,
        "chk_email_verification_tokens_expiry");
    assertTokenRejected(userId, expiresAt, createdAt.minusSeconds(1), null, null, createdAt,
        "chk_email_verification_tokens_consumed_at");
    assertTokenRejected(userId, expiresAt, expiresAt, null, null, createdAt,
        "chk_email_verification_tokens_consumed_at");
    assertTokenRejected(userId, expiresAt, null, createdAt.minusSeconds(1), null, createdAt,
        "chk_email_verification_tokens_revoked_at");
    assertTokenRejected(userId, expiresAt, null, null, createdAt.minusSeconds(1), createdAt,
        "chk_email_verification_tokens_updated_at");
  }

  @Test
  void rejectsTokenThatIsBothConsumedAndRevoked() {
    UUID userId = insertUser();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-05T10:00:00-05:00");

    assertTokenRejected(
        userId,
        createdAt.plusHours(24),
        createdAt.plusMinutes(1),
        createdAt.plusMinutes(2),
        null,
        createdAt,
        "chk_email_verification_tokens_consumed_revoked"
    );
  }

  @Test
  void allowsOnlyOneOpenTokenPerUser() {
    UUID userId = insertUser();
    OffsetDateTime createdAt = OffsetDateTime.now();
    UUID firstTokenId = insertOpenToken(
        userId, tokenHash(), createdAt, createdAt.plusHours(24));

    assertThatThrownBy(() -> insertOpenToken(
        userId, tokenHash(), createdAt, createdAt.plusHours(24)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uq_email_verification_tokens_open_user");

    jdbc.update("""
        UPDATE email_verification_tokens
        SET revoked_at = ?, updated_at = ?
        WHERE id = ?
        """, createdAt.plusMinutes(1), createdAt.plusMinutes(1), firstTokenId);

    UUID secondTokenId = insertOpenToken(
        userId, tokenHash(), createdAt.plusMinutes(1), createdAt.plusHours(24));

    jdbc.update("""
        UPDATE email_verification_tokens
        SET consumed_at = ?, updated_at = ?
        WHERE id = ?
        """, createdAt.plusMinutes(2), createdAt.plusMinutes(2), secondTokenId);

    assertThatCode(() -> insertOpenToken(
        userId, tokenHash(), createdAt.plusMinutes(2), createdAt.plusHours(24)))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsExplicitlyNullAuditTimestamps() {
    UUID userId = insertUser();

    assertThatThrownBy(() -> jdbc.update("""
        INSERT INTO email_verification_tokens (
            id, user_id, token_hash, expires_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, NULL, ?)
        """, UUID.randomUUID(), userId, tokenHash(), OffsetDateTime.now().plusHours(24),
        OffsetDateTime.now()))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("created_at");

    assertThatThrownBy(() -> jdbc.update("""
        INSERT INTO email_verification_tokens (
            id, user_id, token_hash, expires_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, NULL)
        """, UUID.randomUUID(), userId, tokenHash(), OffsetDateTime.now().plusHours(24),
        OffsetDateTime.now()))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("updated_at");
  }

  @Test
  void expiredButUnresolvedTokenStillOccupiesOpenTokenIndex() {
    UUID userId = insertUser();
    OffsetDateTime createdAt = OffsetDateTime.now().minusHours(2);
    insertOpenToken(userId, tokenHash(), createdAt, createdAt.plusHours(1));

    assertThatThrownBy(() -> insertOpenToken(
        userId, tokenHash(), OffsetDateTime.now(), OffsetDateTime.now().plusHours(24)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uq_email_verification_tokens_open_user");
  }

  @Test
  void deletesDependentTokensWithUser() {
    UUID userId = insertUser();
    insertOpenToken(userId, tokenHash(), OffsetDateTime.now(), OffsetDateTime.now().plusHours(24));

    jdbc.update("DELETE FROM users WHERE id = ?", userId);

    Integer tokens = jdbc.queryForObject("""
        SELECT count(*) FROM email_verification_tokens WHERE user_id = ?
        """, Integer.class, userId);
    assertThat(tokens).isZero();
  }

  private UUID insertUser() {
    UUID userId = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO users (id, email, password_hash)
        VALUES (?, ?, ?)
        """, userId, "client-%s@example.com".formatted(userId), "test-password-hash");
    return userId;
  }

  private UUID insertOpenToken(
      UUID userId,
      byte[] hash,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt
  ) {
    UUID tokenId = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO email_verification_tokens (
            id, user_id, token_hash, expires_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?)
        """, tokenId, userId, hash, expiresAt, createdAt, createdAt);
    return tokenId;
  }

  private void assertTokenRejected(
      UUID userId,
      OffsetDateTime expiresAt,
      OffsetDateTime consumedAt,
      OffsetDateTime revokedAt,
      OffsetDateTime updatedAt,
      OffsetDateTime createdAt,
      String constraintName
  ) {
    OffsetDateTime effectiveUpdatedAt = updatedAt == null ? createdAt : updatedAt;
    assertThatThrownBy(() -> jdbc.update("""
        INSERT INTO email_verification_tokens (
            id, user_id, token_hash, expires_at, consumed_at, revoked_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, UUID.randomUUID(), userId, tokenHash(), expiresAt, consumedAt, revokedAt,
        createdAt, effectiveUpdatedAt))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining(constraintName);
  }

  private byte[] tokenHash() {
    byte[] hash = new byte[32];
    RANDOM.nextBytes(hash);
    return hash;
  }
}
