package com.redox.fintechBookingSystem.identity.session;

import com.redox.fintechBookingSystem.identity.authentication.jwt.AccessTokenIssuer;
import com.redox.fintechBookingSystem.identity.authentication.jwt.IssuedAccessToken;
import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.user.Roles;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshServiceTests {
  @Test
  void translatesFailureWithoutIssuingAccessToken() {
    RefreshTransaction transaction = mock(RefreshTransaction.class);
    AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);
    IdentityProperties properties = mock(IdentityProperties.class);
    when(transaction.refresh("raw")).thenReturn(RefreshOutcome.Failure.INVALID_SESSION);
    assertThatThrownBy(() -> new RefreshService(transaction, issuer, properties).refresh("raw"))
        .isInstanceOf(InvalidRefreshSessionException.class);
    verifyNoInteractions(issuer, properties);
  }

  @Test
  void returnsBothAccessLifetimeAndAbsoluteSessionDeadline() {
    RefreshTransaction transaction = mock(RefreshTransaction.class);
    AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);
    IdentityProperties properties = mock(IdentityProperties.class);
    IdentityProperties.Session configuration = mock(IdentityProperties.Session.class);
    UUID userId = UUID.randomUUID(), sessionId = UUID.randomUUID();
    Instant deadline = Instant.parse("2026-11-01T15:00:00Z");
    GeneratedSecureToken replacement = new GeneratedSecureToken("new", new byte[32]);
    when(transaction.refresh("raw")).thenReturn(new RefreshOutcome.Success(
        userId, sessionId, Set.of(Roles.CLIENT), replacement, deadline));
    IssuedAccessToken access = new IssuedAccessToken("jwt", deadline.minusSeconds(1));
    when(issuer.issue(userId, sessionId, Set.of(Roles.CLIENT))).thenReturn(access);
    when(properties.session()).thenReturn(configuration);
    when(configuration.accessTokenTtl()).thenReturn(Duration.ofMinutes(10));
    var result = new RefreshService(transaction, issuer, properties).refresh("raw");
    assertThat(result.accessToken()).isSameAs(access);
    assertThat(result.refreshToken()).isSameAs(replacement);
    assertThat(result.accessTokenExpiresInSeconds()).isEqualTo(600);
    assertThat(result.sessionExpiresAt()).isEqualTo(deadline);
  }
}
