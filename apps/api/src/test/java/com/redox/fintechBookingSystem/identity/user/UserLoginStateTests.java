package com.redox.fintechBookingSystem.identity.user;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserLoginStateTests {
  private static final Instant NOW = Instant.parse("2026-10-09T15:00:00Z");
  private static final Duration WINDOW = Duration.ofMinutes(15);
  private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

  @Test
  void locksAccountWhenMaximumFailuresIsReachedInsideWindow() {
    User user = user();

    for (int attempt = 0; attempt < 5; attempt++) {
      user.recordFailedLogin(NOW.plusSeconds(attempt), 5, WINDOW, LOCK_DURATION);
    }

    assertThat(user.getFailedLoginCount()).isEqualTo(5);
    assertThat(user.getFailureWindowStartedAt()).isEqualTo(NOW);
    assertThat(user.getLockedUntil()).isEqualTo(NOW.plusSeconds(4).plus(LOCK_DURATION));
    assertThat(user.isLoginLockedAt(NOW.plusSeconds(5))).isTrue();
  }

  @Test
  void startsANewWindowWhenPreviousWindowHasElapsed() {
    User user = user();
    user.recordFailedLogin(NOW, 5, WINDOW, LOCK_DURATION);
    user.recordFailedLogin(NOW.plus(WINDOW), 5, WINDOW, LOCK_DURATION);

    assertThat(user.getFailedLoginCount()).isOne();
    assertThat(user.getFailureWindowStartedAt()).isEqualTo(NOW.plus(WINDOW));
    assertThat(user.getLockedUntil()).isNull();
  }

  @Test
  void successfulAuthenticationClearsAllFailureState() {
    User user = user();
    user.recordFailedLogin(NOW, 1, WINDOW, LOCK_DURATION);

    user.clearLoginFailures();

    assertThat(user.getFailedLoginCount()).isZero();
    assertThat(user.getFailureWindowStartedAt()).isNull();
    assertThat(user.getLockedUntil()).isNull();
  }

  private User user() {
    return new User("client@example.com", "password-hash");
  }
}
