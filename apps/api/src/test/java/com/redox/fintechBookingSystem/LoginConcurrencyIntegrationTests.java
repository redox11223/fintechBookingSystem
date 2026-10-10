package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.identity.authentication.LoginOutcome;
import com.redox.fintechBookingSystem.identity.authentication.LoginTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "citafin.identity.password.bcrypt-strength=10",
    "management.health.mail.enabled=false"
})
@ActiveProfiles("test")
class LoginConcurrencyIntegrationTests {
  private static final String CORRECT_PASSWORD = "the correct password phrase";
  private static final String WRONG_PASSWORD = "a different password phrase";

  @Autowired private LoginTransaction loginTransaction;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void concurrentFailuresAreSerializedAndNeitherIncrementIsLost() throws Exception {
    String email = "concurrent-login-%s@example.com".formatted(UUID.randomUUID());
    UUID userId = insertVerifiedUser(email);
    CyclicBarrier startTogether = new CyclicBarrier(2);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<LoginOutcome> first = executor.submit(
          () -> attemptAfterBarrier(email, startTogether));
      Future<LoginOutcome> second = executor.submit(
          () -> attemptAfterBarrier(email, startTogether));

      assertThat(first.get(15, TimeUnit.SECONDS))
          .isEqualTo(LoginOutcome.Failure.INVALID_CREDENTIALS);
      assertThat(second.get(15, TimeUnit.SECONDS))
          .isEqualTo(LoginOutcome.Failure.INVALID_CREDENTIALS);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    Integer failures = jdbc.queryForObject(
        "SELECT failed_login_count FROM users WHERE id = ?", Integer.class, userId);
    OffsetDateTime windowStartedAt = jdbc.queryForObject(
        "SELECT failure_window_started_at FROM users WHERE id = ?", OffsetDateTime.class, userId);
    assertThat(failures).isEqualTo(2);
    assertThat(windowStartedAt).isNotNull();
  }

  private LoginOutcome attemptAfterBarrier(String email, CyclicBarrier barrier) {
    try {
      barrier.await(10, TimeUnit.SECONDS);
      return loginTransaction.attempt(email, WRONG_PASSWORD);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private UUID insertVerifiedUser(String email) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.update("""
        INSERT INTO users (
            id, email, password_hash, email_verified_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?)
        """,
        id,
        email,
        passwordEncoder.encode(CORRECT_PASSWORD),
        OffsetDateTime.ofInstant(now.minusSeconds(1), ZoneOffset.UTC),
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC),
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
    return id;
  }
}
