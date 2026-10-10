package com.redox.fintechBookingSystem.identity.authentication;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.password.InvalidPasswordException;
import com.redox.fintechBookingSystem.identity.password.PasswordPolicy;
import com.redox.fintechBookingSystem.identity.password.PasswordViolation;
import com.redox.fintechBookingSystem.identity.session.AuthSession;
import com.redox.fintechBookingSystem.identity.session.AuthSessionRepo;
import com.redox.fintechBookingSystem.identity.session.RefreshToken;
import com.redox.fintechBookingSystem.identity.session.RefreshTokenRepo;
import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import com.redox.fintechBookingSystem.identity.user.Roles;
import com.redox.fintechBookingSystem.identity.user.User;
import com.redox.fintechBookingSystem.identity.user.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginTransactionTests {
  private static final Instant NOW = Instant.parse("2026-10-09T15:00:00Z");
  private static final UUID USER_ID = UUID.fromString("3dc03b50-a62d-4767-832d-243e445eb665");
  private static final UUID SESSION_ID = UUID.fromString("cb96bbb9-d246-4090-a068-7254ef047d0b");
  private static final String RAW_PASSWORD = "a sufficiently long password";
  private static final String NORMALIZED_PASSWORD = "a sufficiently long password";
  private static final String PASSWORD_HASH = "$2a$10$real-password-hash";
  private static final String DUMMY_HASH = "$2a$10$dummy-password-hash";
  private static final Duration REFRESH_TTL = Duration.ofDays(30);

  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserRepo userRepo;
  @Mock private PasswordPolicy passwordPolicy;
  @Mock private IdentityProperties properties;
  @Mock private IdentityProperties.Login loginProperties;
  @Mock private IdentityProperties.Session sessionProperties;
  @Mock private SecureTokenGenerator tokenGenerator;
  @Mock private AuthSessionRepo authSessionRepo;
  @Mock private RefreshTokenRepo refreshTokenRepo;

  private LoginTransaction transaction;

  @BeforeEach
  void setUp() {
    when(passwordEncoder.encode("this-is-a-dummy-password-only-for-timing"))
        .thenReturn(DUMMY_HASH);
    transaction = new LoginTransaction(
        passwordEncoder,
        userRepo,
        passwordPolicy,
        properties,
        tokenGenerator,
        authSessionRepo,
        refreshTokenRepo,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void invalidPasswordShapePerformsDummyHashWorkAndHidesPolicyFailure() {
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD))
        .thenThrow(new InvalidPasswordException(PasswordViolation.TOO_SHORT));
    when(passwordEncoder.matches("this-is-a-dummy-password-only-for-timing", DUMMY_HASH))
        .thenReturn(true);

    LoginOutcome result = transaction.attempt("client@example.com", RAW_PASSWORD);

    assertThat(result).isEqualTo(LoginOutcome.Failure.INVALID_CREDENTIALS);
    verify(passwordEncoder).matches("this-is-a-dummy-password-only-for-timing", DUMMY_HASH);
    verifyNoInteractions(userRepo, tokenGenerator, authSessionRepo, refreshTokenRepo);
  }

  @Test
  void unknownEmailPerformsPasswordCheckAgainstDummyHash() {
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD)).thenReturn(NORMALIZED_PASSWORD);
    when(userRepo.findByEmail("client@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.matches(NORMALIZED_PASSWORD, DUMMY_HASH)).thenReturn(false);

    LoginOutcome result = transaction.attempt("  CLIENT@Example.COM  ", RAW_PASSWORD);

    assertThat(result).isEqualTo(LoginOutcome.Failure.INVALID_CREDENTIALS);
    verify(passwordEncoder).matches(NORMALIZED_PASSWORD, DUMMY_HASH);
    verifyNoInteractions(tokenGenerator, authSessionRepo, refreshTokenRepo);
  }

  @Test
  void wrongPasswordRecordsFailureWithoutCreatingSession() {
    User user = clientUser();
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD)).thenReturn(NORMALIZED_PASSWORD);
    when(userRepo.findByEmail("client@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(NORMALIZED_PASSWORD, PASSWORD_HASH)).thenReturn(false);
    when(properties.login()).thenReturn(loginProperties);
    when(loginProperties.maxFailures()).thenReturn(5);
    when(loginProperties.failureWindow()).thenReturn(Duration.ofMinutes(15));
    when(loginProperties.lockDuration()).thenReturn(Duration.ofMinutes(15));

    LoginOutcome result = transaction.attempt("client@example.com", RAW_PASSWORD);

    assertThat(result).isEqualTo(LoginOutcome.Failure.INVALID_CREDENTIALS);
    assertThat(user.getFailedLoginCount()).isOne();
    assertThat(user.getFailureWindowStartedAt()).isEqualTo(NOW);
    verifyNoInteractions(tokenGenerator, authSessionRepo, refreshTokenRepo);
  }

  @Test
  void successfulClientLoginClearsFailuresAndCreatesOneSessionAndRefreshToken() {
    User user = verifiedClientUser();
    user.recordFailedLogin(
        NOW.minusSeconds(1), 5, Duration.ofMinutes(15), Duration.ofMinutes(15));
    GeneratedSecureToken generated =
        new GeneratedSecureToken("raw-refresh-token", new byte[32]);
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD)).thenReturn(NORMALIZED_PASSWORD);
    when(userRepo.findByEmail("client@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(NORMALIZED_PASSWORD, PASSWORD_HASH)).thenReturn(true);
    when(properties.session()).thenReturn(sessionProperties);
    when(sessionProperties.refreshTokenTtl()).thenReturn(REFRESH_TTL);
    when(authSessionRepo.save(any(AuthSession.class))).thenAnswer(invocation -> {
      AuthSession session = invocation.getArgument(0);
      ReflectionTestUtils.setField(session, "id", SESSION_ID);
      return session;
    });
    when(tokenGenerator.generate()).thenReturn(generated);

    LoginOutcome result = transaction.attempt("client@example.com", RAW_PASSWORD);

    assertThat(result).isEqualTo(new LoginOutcome.Success(
        USER_ID, SESSION_ID, user.getRoles(), generated));
    assertThat(user.getFailedLoginCount()).isZero();
    assertThat(user.getFailureWindowStartedAt()).isNull();
    assertThat(user.getLockedUntil()).isNull();

    ArgumentCaptor<AuthSession> sessionCaptor = ArgumentCaptor.forClass(AuthSession.class);
    verify(authSessionRepo).save(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getUser()).isSameAs(user);
    assertThat(sessionCaptor.getValue().getAbsoluteExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));

    ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepo).save(tokenCaptor.capture());
    assertThat(tokenCaptor.getValue().getSession()).isSameAs(sessionCaptor.getValue());
    assertThat(tokenCaptor.getValue().getTokenHash()).containsExactly(new byte[32]);
    assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
  }

  @Test
  void validPasswordForDisabledAccountDoesNotCreateSession() {
    User user = verifiedClientUser();
    ReflectionTestUtils.setField(user, "isActive", false);
    prepareMatchingPassword(user);

    assertThat(transaction.attempt("client@example.com", RAW_PASSWORD))
        .isEqualTo(LoginOutcome.Failure.ACCOUNT_DISABLED);

    verifyNoInteractions(tokenGenerator, authSessionRepo, refreshTokenRepo);
  }

  @Test
  void validPasswordForUnverifiedAccountDoesNotCreateSession() {
    User user = clientUser();
    prepareMatchingPassword(user);

    assertThat(transaction.attempt("client@example.com", RAW_PASSWORD))
        .isEqualTo(LoginOutcome.Failure.EMAIL_NOT_VERIFIED);

    verifyNoInteractions(tokenGenerator, authSessionRepo, refreshTokenRepo);
  }

  @Test
  void privilegedAccountRequiresMfaBeforeCreatingSession() {
    User user = clientUser();
    user.verifyEmailAt(NOW.minusSeconds(1));
    user.addRole(Roles.ADVISOR);
    prepareMatchingPassword(user);

    assertThat(transaction.attempt("client@example.com", RAW_PASSWORD))
        .isEqualTo(LoginOutcome.Failure.MFA_REQUIRED);

    verifyNoInteractions(tokenGenerator, authSessionRepo, refreshTokenRepo);
  }

  private void prepareMatchingPassword(User user) {
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD)).thenReturn(NORMALIZED_PASSWORD);
    when(userRepo.findByEmail("client@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(NORMALIZED_PASSWORD, PASSWORD_HASH)).thenReturn(true);
  }

  private User verifiedClientUser() {
    User user = clientUser();
    user.verifyEmailAt(NOW.minusSeconds(1));
    return user;
  }

  private User clientUser() {
    User user = new User("client@example.com", PASSWORD_HASH);
    user.addRole(Roles.CLIENT);
    ReflectionTestUtils.setField(user, "id", USER_ID);
    return user;
  }
}
