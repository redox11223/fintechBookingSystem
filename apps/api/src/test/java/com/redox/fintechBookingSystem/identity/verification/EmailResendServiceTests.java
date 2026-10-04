package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.User;
import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailResendServiceTests {
  private static final Instant NOW = Instant.parse("2026-10-04T15:30:00Z");
  private static final Duration COOLDOWN = Duration.ofMinutes(1);
  private static final Duration TOKEN_TTL = Duration.ofHours(24);
  private static final UUID USER_ID = UUID.fromString("29c33162-595c-41dd-bac4-6635152d2583");
  private static final byte[] TOKEN_HASH = new byte[32];

  @Mock private UserRepo userRepo;
  @Mock private EmailVerificationTokenRepo tokenRepo;
  @Mock private IdentityProperties identityProperties;
  @Mock private IdentityProperties.Tokens tokenProperties;
  @Mock private EmailVerificationTokenGenerator tokenGenerator;
  @Mock private ApplicationEventPublisher eventPublisher;

  private EmailResendService service;

  @BeforeEach
  void setUp() {
    service = new EmailResendService(
        userRepo,
        tokenRepo,
        identityProperties,
        tokenGenerator,
        Clock.fixed(NOW, ZoneOffset.UTC),
        eventPublisher);
  }

  @Test
  void unknownEmailEndsWithoutObservableWork() {
    when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    service.resend("  MISSING@Example.COM  ");

    verify(userRepo).findByEmail("missing@example.com");
    verifyNoInteractions(tokenRepo, identityProperties, tokenGenerator, eventPublisher);
  }

  @Test
  void verifiedUserEndsWithoutLookingForAToken() {
    User user = userWithId("verified@example.com");
    user.verifyEmailAt(NOW.minusSeconds(1));
    when(userRepo.findByEmail("verified@example.com")).thenReturn(Optional.of(user));

    service.resend("verified@example.com");

    verifyNoInteractions(tokenRepo, identityProperties, tokenGenerator, eventPublisher);
  }

  @Test
  void openTokenInsideCooldownIsKept() {
    User user = userWithId("client@example.com");
    EmailVerificationToken openToken = tokenCreatedAt(user, NOW.minusSeconds(30));
    when(userRepo.findByEmail("client@example.com")).thenReturn(Optional.of(user));
    when(tokenRepo.findOpenTokenByUserId(USER_ID)).thenReturn(Optional.of(openToken));
    when(identityProperties.tokens()).thenReturn(tokenProperties);
    when(tokenProperties.resendCooldown()).thenReturn(COOLDOWN);

    service.resend("client@example.com");

    assertThat(ReflectionTestUtils.getField(openToken, "revokedAt")).isNull();
    verify(tokenRepo, never()).flush();
    verify(tokenRepo, never()).save(any());
    verifyNoInteractions(tokenGenerator, eventPublisher);
  }

  @Test
  void replacesOpenTokenWhenCooldownHasElapsed() {
    User user = userWithId("client@example.com");
    EmailVerificationToken openToken = tokenCreatedAt(user, NOW.minus(COOLDOWN));
    GeneratedEmailVerificationToken generated =
        new GeneratedEmailVerificationToken("new-raw-token", TOKEN_HASH);
    when(userRepo.findByEmail("client@example.com")).thenReturn(Optional.of(user));
    when(tokenRepo.findOpenTokenByUserId(USER_ID)).thenReturn(Optional.of(openToken));
    when(identityProperties.tokens()).thenReturn(tokenProperties);
    when(tokenProperties.resendCooldown()).thenReturn(COOLDOWN);
    when(tokenProperties.emailVerificationTtl()).thenReturn(TOKEN_TTL);
    when(tokenGenerator.generate()).thenReturn(generated);

    service.resend("CLIENT@example.com");

    assertThat(ReflectionTestUtils.getField(openToken, "revokedAt")).isEqualTo(NOW);
    ArgumentCaptor<EmailVerificationToken> tokenCaptor =
        ArgumentCaptor.forClass(EmailVerificationToken.class);
    ArgumentCaptor<EmailVerificationRequested> eventCaptor =
        ArgumentCaptor.forClass(EmailVerificationRequested.class);
    InOrder order = inOrder(tokenRepo, eventPublisher);
    order.verify(tokenRepo).flush();
    order.verify(tokenRepo).save(tokenCaptor.capture());
    order.verify(eventPublisher).publishEvent(eventCaptor.capture());

    EmailVerificationToken newToken = tokenCaptor.getValue();
    assertThat(ReflectionTestUtils.getField(newToken, "user")).isSameAs(user);
    assertThat((byte[]) ReflectionTestUtils.getField(newToken, "tokenHash"))
        .containsExactly(TOKEN_HASH);
    assertThat(ReflectionTestUtils.getField(newToken, "expiresAt"))
        .isEqualTo(NOW.plus(TOKEN_TTL));
    assertThat(eventCaptor.getValue().email()).isEqualTo("client@example.com");
    assertThat(eventCaptor.getValue().rawToken()).isEqualTo("new-raw-token");
  }

  @Test
  void createsTokenImmediatelyWhenNoOpenTokenExists() {
    User user = userWithId("client@example.com");
    when(userRepo.findByEmail("client@example.com")).thenReturn(Optional.of(user));
    when(tokenRepo.findOpenTokenByUserId(USER_ID)).thenReturn(Optional.empty());
    when(identityProperties.tokens()).thenReturn(tokenProperties);
    when(tokenProperties.emailVerificationTtl()).thenReturn(TOKEN_TTL);
    when(tokenGenerator.generate()).thenReturn(
        new GeneratedEmailVerificationToken("new-raw-token", TOKEN_HASH));

    service.resend("client@example.com");

    verify(tokenRepo, never()).flush();
    verify(tokenRepo).save(any(EmailVerificationToken.class));
    verify(eventPublisher).publishEvent(any(EmailVerificationRequested.class));
  }

  private User userWithId(String email) {
    User user = new User(email, "password-hash");
    ReflectionTestUtils.setField(user, "id", USER_ID);
    return user;
  }

  private EmailVerificationToken tokenCreatedAt(User user, Instant createdAt) {
    EmailVerificationToken token =
        new EmailVerificationToken(user, new byte[32], NOW.plus(TOKEN_TTL));
    ReflectionTestUtils.setField(token, "createdAt", createdAt);
    return token;
  }
}
