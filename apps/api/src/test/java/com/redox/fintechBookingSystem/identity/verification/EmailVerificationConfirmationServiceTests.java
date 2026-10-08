package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import com.redox.fintechBookingSystem.identity.user.User;
import com.redox.fintechBookingSystem.identity.user.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationConfirmationServiceTests {
  private static final Instant NOW = Instant.parse("2026-09-22T15:30:00Z");
  private static final String RAW_TOKEN = "raw-verification-token";
  private static final byte[] TOKEN_HASH = new byte[32];
  private static final UUID USER_ID = UUID.fromString("ee749af0-262c-4be4-bfde-f2caf163c6ac");

  @Mock private SecureTokenGenerator tokenGenerator;
  @Mock private EmailVerificationTokenConsumptionRepo tokenConsumptionRepo;
  @Mock private UserRepo userRepo;

  private EmailVerificationConfirmationService service;

  @BeforeEach
  void setUp() {
    service = new EmailVerificationConfirmationService(
        tokenGenerator,
        tokenConsumptionRepo,
        userRepo,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void consumesValidTokenAndVerifiesItsUserAtTheSameInstant() {
    User user = new User("client@example.com", "password-hash");
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(tokenConsumptionRepo.consumeIfValid(TOKEN_HASH, NOW)).thenReturn(Optional.of(USER_ID));
    when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

    service.confirm(RAW_TOKEN);

    assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    verify(tokenGenerator).hash(RAW_TOKEN);
    verify(tokenConsumptionRepo).consumeIfValid(TOKEN_HASH, NOW);
    verify(userRepo).findById(USER_ID);
  }

  @Test
  void rejectsInvalidTokenWithoutLookingUpAUser() {
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(tokenConsumptionRepo.consumeIfValid(TOKEN_HASH, NOW)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirm(RAW_TOKEN))
        .isInstanceOf(InvalidEmailVerificationTokenException.class)
        .hasMessage("Email verification token is invalid");

    verifyNoInteractions(userRepo);
  }

  @Test
  void usesTheSamePublicErrorWhenConsumedTokenReferencesNoUser() {
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(tokenConsumptionRepo.consumeIfValid(TOKEN_HASH, NOW)).thenReturn(Optional.of(USER_ID));
    when(userRepo.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirm(RAW_TOKEN))
        .isInstanceOf(InvalidEmailVerificationTokenException.class)
        .hasMessage("Email verification token is invalid");
  }
}
