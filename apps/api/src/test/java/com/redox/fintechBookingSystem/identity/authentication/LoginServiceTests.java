package com.redox.fintechBookingSystem.identity.authentication;

import com.redox.fintechBookingSystem.identity.authentication.exception.AccountDisabledException;
import com.redox.fintechBookingSystem.identity.authentication.exception.AuthenticationFailedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.EmailNotVerifiedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.MfaRequiredException;
import com.redox.fintechBookingSystem.identity.authentication.jwt.AccessTokenIssuer;
import com.redox.fintechBookingSystem.identity.authentication.jwt.IssuedAccessToken;
import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.user.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTests {
  private static final UUID USER_ID = UUID.fromString("a9898d46-a272-498f-bfb1-0dc55633bd3c");
  private static final UUID SESSION_ID = UUID.fromString("e2732667-8e3e-44fc-b684-c84626963230");
  private static final GeneratedSecureToken REFRESH_TOKEN =
      new GeneratedSecureToken("raw-refresh-token", new byte[32]);

  @Mock private AccessTokenIssuer tokenIssuer;
  @Mock private LoginTransaction loginTransaction;
  @Mock private IdentityProperties properties;
  @Mock private IdentityProperties.Session sessionProperties;

  private LoginService loginService;

  @BeforeEach
  void setUp() {
    loginService = new LoginService(tokenIssuer, loginTransaction, properties);
  }

  @Test
  void issuesAccessTokenAfterSuccessfulTransactionalAttempt() {
    LoginOutcome.Success success = new LoginOutcome.Success(
        USER_ID, SESSION_ID, Set.of(Roles.CLIENT), REFRESH_TOKEN);
    IssuedAccessToken accessToken =
        new IssuedAccessToken("signed-access-token", Instant.parse("2026-10-09T15:10:00Z"));
    when(loginTransaction.attempt("client@example.com", "valid password phrase"))
        .thenReturn(success);
    when(tokenIssuer.issue(USER_ID, SESSION_ID, Set.of(Roles.CLIENT))).thenReturn(accessToken);
    when(properties.session()).thenReturn(sessionProperties);
    when(sessionProperties.accessTokenTtl()).thenReturn(Duration.ofMinutes(10));

    AuthenticatedLogin result =
        loginService.login("client@example.com", "valid password phrase");

    assertThat(result.accessToken()).isSameAs(accessToken);
    assertThat(result.refreshToken()).isSameAs(REFRESH_TOKEN);
    assertThat(result.accessTokenExpiresInSeconds()).isEqualTo(600);
  }

  @Test
  void mapsInvalidCredentialsToPublicAuthenticationFailure() {
    assertFailure(LoginOutcome.Failure.INVALID_CREDENTIALS, AuthenticationFailedException.class);
  }

  @Test
  void mapsDisabledAccountToDedicatedFailure() {
    assertFailure(LoginOutcome.Failure.ACCOUNT_DISABLED, AccountDisabledException.class);
  }

  @Test
  void mapsUnverifiedEmailToDedicatedFailure() {
    assertFailure(LoginOutcome.Failure.EMAIL_NOT_VERIFIED, EmailNotVerifiedException.class);
  }

  @Test
  void mapsMfaRequirementToDedicatedFailure() {
    assertFailure(LoginOutcome.Failure.MFA_REQUIRED, MfaRequiredException.class);
  }

  private void assertFailure(LoginOutcome.Failure failure,
                             Class<? extends RuntimeException> expectedType) {
    when(loginTransaction.attempt("client@example.com", "password")).thenReturn(failure);

    assertThatThrownBy(() -> loginService.login("client@example.com", "password"))
        .isInstanceOf(expectedType);

    verifyNoInteractions(tokenIssuer, properties);
  }
}
