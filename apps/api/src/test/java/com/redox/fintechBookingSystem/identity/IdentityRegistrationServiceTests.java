package com.redox.fintechBookingSystem.identity;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.password.InvalidPasswordException;
import com.redox.fintechBookingSystem.identity.password.PasswordPolicy;
import com.redox.fintechBookingSystem.identity.password.PasswordViolation;
import com.redox.fintechBookingSystem.identity.repo.UserRepo;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationToken;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenGenerator;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenRepo;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationRequested;
import com.redox.fintechBookingSystem.identity.verification.GeneratedEmailVerificationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityRegistrationServiceTests {
  private static final String RAW_PASSWORD = "contrasena de prueba larga";
  private static final String NORMALIZED_PASSWORD = "contraseña de prueba larga";
  private static final String PASSWORD_HASH = "$2a$12$encoded-password";
  private static final byte[] TOKEN_HASH = new byte[32];
  private static final Duration TOKEN_TTL = Duration.ofHours(24);
  private static final Instant NOW = Instant.parse("2026-09-10T15:00:00Z");

  @Mock private UserRepo userRepo;
  @Mock private PasswordPolicy passwordPolicy;
  @Mock private EmailVerificationTokenGenerator tokenGenerator;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private IdentityProperties identityProperties;
  @Mock private IdentityProperties.Tokens tokenProperties;
  @Mock private EmailVerificationTokenRepo verificationTokenRepo;
  @Mock private ApplicationEventPublisher eventPublisher;

  private IdentityRegistrationService registrationService;

  @BeforeEach
  void setUp() {
    registrationService = new IdentityRegistrationService(
        userRepo,
        passwordPolicy,
        tokenGenerator,
        passwordEncoder,
        identityProperties,
        verificationTokenRepo,
        Clock.fixed(NOW, ZoneOffset.UTC),
        eventPublisher);
  }

  @Test
  void registersPendingClientIdentityWithHashedSecretsAndConfiguredExpiration() {
    User savedUser = new User();
    GeneratedEmailVerificationToken generatedToken =
        new GeneratedEmailVerificationToken("raw-verification-token", TOKEN_HASH);
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD)).thenReturn(NORMALIZED_PASSWORD);
    when(passwordEncoder.encode(NORMALIZED_PASSWORD)).thenReturn(PASSWORD_HASH);
    when(userRepo.existsByEmail("client@example.com")).thenReturn(false);
    when(userRepo.save(any(User.class))).thenReturn(savedUser);
    when(tokenGenerator.generate()).thenReturn(generatedToken);
    when(identityProperties.tokens()).thenReturn(tokenProperties);
    when(tokenProperties.emailVerificationTtl()).thenReturn(TOKEN_TTL);

    Optional<User> result = registrationService.registerPendingUser(
        "  CLIENT@Example.COM  ", RAW_PASSWORD);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(userCaptor.capture());
    User userToPersist = userCaptor.getValue();
    assertThat(userToPersist.getEmail()).isEqualTo("client@example.com");
    assertThat(userToPersist.getPassword()).isEqualTo(PASSWORD_HASH);
    assertThat(userToPersist.getRoles()).containsExactly(Roles.CLIENT);
    assertThat(userToPersist.getEmailVerifiedAt()).isNull();
    assertThat(userToPersist.isActive()).isTrue();
    assertThat(result).containsSame(savedUser);

    ArgumentCaptor<EmailVerificationToken> tokenCaptor =
        ArgumentCaptor.forClass(EmailVerificationToken.class);
    ArgumentCaptor<EmailVerificationRequested> eventCaptor =
        ArgumentCaptor.forClass(EmailVerificationRequested.class);
    InOrder persistenceOrder = inOrder(verificationTokenRepo, eventPublisher);
    persistenceOrder.verify(verificationTokenRepo).save(tokenCaptor.capture());
    persistenceOrder.verify(eventPublisher).publishEvent(eventCaptor.capture());
    EmailVerificationToken tokenToPersist = tokenCaptor.getValue();
    assertThat(ReflectionTestUtils.getField(tokenToPersist, "user")).isSameAs(savedUser);
    assertThat((byte[]) ReflectionTestUtils.getField(tokenToPersist, "tokenHash"))
        .containsExactly(TOKEN_HASH);
    assertThat(ReflectionTestUtils.getField(tokenToPersist, "expiresAt"))
        .isEqualTo(NOW.plus(TOKEN_TTL));
    assertThat(eventCaptor.getValue().email()).isEqualTo("client@example.com");
    assertThat(eventCaptor.getValue().rawToken()).isEqualTo("raw-verification-token");
  }

  @Test
  void existingEmailStillValidatesAndHashesPasswordWithoutPersistingAnything() {
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD)).thenReturn(NORMALIZED_PASSWORD);
    when(passwordEncoder.encode(NORMALIZED_PASSWORD)).thenReturn(PASSWORD_HASH);
    when(userRepo.existsByEmail("client@example.com")).thenReturn(true);

    Optional<User> result = registrationService.registerPendingUser(
        "CLIENT@example.com", RAW_PASSWORD);

    assertThat(result).isEmpty();
    InOrder securityOrder = inOrder(passwordPolicy, passwordEncoder, userRepo);
    securityOrder.verify(passwordPolicy).validateAndNormalize(RAW_PASSWORD);
    securityOrder.verify(passwordEncoder).encode(NORMALIZED_PASSWORD);
    securityOrder.verify(userRepo).existsByEmail("client@example.com");
    verify(userRepo, never()).save(any(User.class));
    verifyNoInteractions(tokenGenerator, verificationTokenRepo, eventPublisher);
  }

  @Test
  void invalidPasswordStopsBeforeLookupAndPersistence() {
    InvalidPasswordException rejection =
        new InvalidPasswordException(PasswordViolation.TOO_SHORT);
    when(passwordPolicy.validateAndNormalize(RAW_PASSWORD)).thenThrow(rejection);

    assertThatThrownBy(() -> registrationService.registerPendingUser(
        "client@example.com", RAW_PASSWORD))
        .isSameAs(rejection);

    verifyNoInteractions(
        passwordEncoder, userRepo, tokenGenerator, verificationTokenRepo, eventPublisher);
  }

  @Test
  void rejectsBlankEmailBeforeUsingCollaborators() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> registrationService.registerPendingUser(" \t", RAW_PASSWORD))
        .withMessage("Email is required");

    verifyNoInteractions(
        passwordPolicy, passwordEncoder, userRepo, tokenGenerator, verificationTokenRepo,
        eventPublisher);
  }
}
