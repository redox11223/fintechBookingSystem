package com.redox.fintechBookingSystem.identity.session;

import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import com.redox.fintechBookingSystem.identity.user.Roles;
import com.redox.fintechBookingSystem.identity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RefreshTransactionTests {
  private static final Instant NOW = Instant.parse("2026-10-14T15:00:00Z");
  private static final byte[] HASH = new byte[32];
  @Mock SecureTokenGenerator generator;
  @Mock AuthSessionRepo sessions;
  @Mock RefreshTokenRepo tokens;
  @Mock Clock clock;
  RefreshTransaction transaction;
  AuthSession session;
  RefreshToken token;

  @BeforeEach
  void setup() {
    User user = new User("client@example.com", "hash");
    user.addRole(Roles.CLIENT);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    session = new AuthSession(user, NOW.plusSeconds(3600));
    ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
    token = new RefreshToken(session, HASH, session.getAbsoluteExpiresAt());
    transaction = new RefreshTransaction(generator, sessions, tokens, clock);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void missingTokenReturnsFailureWithoutDatabaseAccess(String raw) {
    assertThat(transaction.refresh(raw)).isEqualTo(RefreshOutcome.Failure.INVALID_SESSION);
    verifyNoInteractions(generator, sessions, tokens, clock);
  }

  @Test
  void unknownTokenReturnsFailure() {
    when(generator.hash("raw")).thenReturn(HASH);
    when(sessions.findByTokenHashForUpdate(HASH)).thenReturn(Optional.empty());
    assertThat(transaction.refresh("raw")).isEqualTo(RefreshOutcome.Failure.INVALID_SESSION);
    verifyNoInteractions(tokens, clock);
  }

  private void foundToken() {
    when(generator.hash("raw")).thenReturn(HASH);
    when(sessions.findByTokenHashForUpdate(HASH)).thenReturn(Optional.of(session));
    when(tokens.findByTokenHash(HASH)).thenReturn(Optional.of(token));
    when(clock.instant()).thenReturn(NOW);
  }

  @Test
  void consumedTokenRevokesSessionWithoutThrowingOrCreatingReplacement() {
    token.consumeAt(NOW.minusSeconds(1));
    foundToken();
    assertThat(transaction.refresh("raw")).isEqualTo(RefreshOutcome.Failure.INVALID_SESSION);
    assertThat(session.getRevokedAt()).isEqualTo(NOW);
    assertThat(token.isConsumed()).isTrue();
    assertThat(token.isRevoked()).isFalse();
    verify(generator, never()).generate();
    verify(tokens, never()).save(any());
  }

  @Test
  void revokedSessionAndExpiredOrRevokedTokensCannotRotate() {
    foundToken();
    token.revokeAt(NOW.minusSeconds(1));
    assertThat(transaction.refresh("raw")).isEqualTo(RefreshOutcome.Failure.INVALID_SESSION);
    token = new RefreshToken(session, HASH, NOW);
    when(tokens.findByTokenHash(HASH)).thenReturn(Optional.of(token));
    assertThat(transaction.refresh("raw")).isEqualTo(RefreshOutcome.Failure.INVALID_SESSION);
    session.revokeAt(NOW.minusSeconds(1));
    assertThat(transaction.refresh("raw")).isEqualTo(RefreshOutcome.Failure.INVALID_SESSION);
    verify(generator, never()).generate();
  }

  @Test
  void rotationReadsTimeAfterLockAndFlushesBeforeInsertingWithUnchangedDeadline() {
    foundToken();
    GeneratedSecureToken generated = new GeneratedSecureToken("replacement", new byte[32]);
    when(generator.generate()).thenReturn(generated);
    RefreshOutcome.Success result = (RefreshOutcome.Success) transaction.refresh("raw");
    assertThat(result.absoluteExpiresAt()).isEqualTo(session.getAbsoluteExpiresAt());
    assertThat(token.getConsumedAt()).isEqualTo(NOW);
    var order = inOrder(sessions, tokens, clock);
    order.verify(sessions).findByTokenHashForUpdate(HASH);
    order.verify(tokens).findByTokenHash(HASH);
    order.verify(clock).instant();
    order.verify(tokens).flush();
    order.verify(tokens).save(any(RefreshToken.class));
  }

  @Test
  void exactExpirationCannotBeConsumed() {
    RefreshToken expired = new RefreshToken(session, HASH, NOW);
    assertThat(expired.isValidAt(NOW)).isFalse();
    assertThatThrownBy(() -> expired.consumeAt(NOW)).isInstanceOf(IllegalStateException.class);
    assertThat(expired.getConsumedAt()).isNull();
  }
}
