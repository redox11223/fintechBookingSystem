package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.identity.session.*;
import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class RefreshRotationIntegrationTests {
  @Autowired RefreshTransaction transaction;
  @Autowired RefreshService service;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean SecureTokenGenerator generator;

  @Test
  void rotationKeepsDeadlineAndReuseRevocationCommitsBeforePublicException() {
    Fixture original = fixture();
    var rotated = service.refresh(original.rawToken());
    assertThat(rotated.sessionExpiresAt()).isEqualTo(original.expiresAt());
    assertThat(openTokens(original.sessionId())).isEqualTo(1);
    assertThatThrownBy(() -> service.refresh(original.rawToken()))
        .isInstanceOf(InvalidRefreshSessionException.class);
    assertThat(jdbc.queryForObject("SELECT revoked_at FROM auth_sessions WHERE id = ?",
        OffsetDateTime.class, original.sessionId())).isNotNull();
    assertThatThrownBy(() -> service.refresh(rotated.refreshToken().rawToken()))
        .isInstanceOf(InvalidRefreshSessionException.class);
    assertThat(tokenCount(original.sessionId())).isEqualTo(2);
  }

  @Test
  void failureAfterFlushRollsBackConsumption() {
    Fixture original = fixture();
    doThrow(new IllegalStateException("simulated generation failure")).when(generator).generate();
    assertThatThrownBy(() -> transaction.refresh(original.rawToken()))
        .isInstanceOf(IllegalStateException.class).hasMessage("simulated generation failure");
    assertThat(jdbc.queryForObject("SELECT consumed_at FROM refresh_tokens WHERE session_id = ?",
        OffsetDateTime.class, original.sessionId())).isNull();
    assertThat(openTokens(original.sessionId())).isEqualTo(1);
    assertThat(tokenCount(original.sessionId())).isEqualTo(1);
  }

  @Test
  void twoConcurrentRefreshesCreateOneReplacementAndRevokeOnlyTheirSession() throws Exception {
    Fixture original = fixture();
    Fixture independent = fixture();
    CyclicBarrier barrier = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<RefreshOutcome> attempt = () -> {
        barrier.await(10, TimeUnit.SECONDS);
        return transaction.refresh(original.rawToken());
      };
      Future<RefreshOutcome> first = executor.submit(attempt);
      Future<RefreshOutcome> second = executor.submit(attempt);
      assertThat(java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
          .filteredOn(result -> result instanceof RefreshOutcome.Success).hasSize(1);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
    assertThat(tokenCount(original.sessionId())).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT revoked_at FROM auth_sessions WHERE id = ?",
        OffsetDateTime.class, original.sessionId())).isNotNull();
    assertThat(transaction.refresh(independent.rawToken())).isInstanceOf(RefreshOutcome.Success.class);
  }

  private int openTokens(UUID sessionId) {
    return jdbc.queryForObject("""
        SELECT count(*) FROM refresh_tokens
        WHERE session_id = ? AND consumed_at IS NULL AND revoked_at IS NULL
        """, Integer.class, sessionId);
  }

  private int tokenCount(UUID sessionId) {
    return jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE session_id = ?",
        Integer.class, sessionId);
  }

  private Fixture fixture() {
    UUID userId = UUID.randomUUID(), sessionId = UUID.randomUUID();
    Instant created = Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    Instant expiration = created.plus(Duration.ofDays(30));
    OffsetDateTime createdAt = created.atOffset(ZoneOffset.UTC);
    OffsetDateTime expiresAt = expiration.atOffset(ZoneOffset.UTC);
    var token = generator.generate();
    jdbc.update("""
        INSERT INTO users (id, email, password_hash, email_verified_at, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """, userId, userId + "@example.com", "unused-password-hash", createdAt, createdAt, createdAt);
    jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'CLIENT')", userId);
    jdbc.update("""
        INSERT INTO auth_sessions (id, user_id, absolute_expires_at, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?)
        """, sessionId, userId, expiresAt, createdAt, createdAt);
    jdbc.update("""
        INSERT INTO refresh_tokens (id, session_id, token_hash, expires_at, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """, UUID.randomUUID(), sessionId, token.tokenHash(), expiresAt, createdAt, createdAt);
    return new Fixture(sessionId, token.rawToken(), expiration);
  }

  private record Fixture(UUID sessionId, String rawToken, Instant expiresAt) {}
}
