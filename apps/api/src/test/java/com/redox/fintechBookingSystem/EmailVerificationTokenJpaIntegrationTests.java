package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.identity.Roles;
import com.redox.fintechBookingSystem.identity.User;
import com.redox.fintechBookingSystem.identity.repo.UserRepo;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationToken;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmailVerificationTokenJpaIntegrationTests {

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private EmailVerificationTokenRepo tokenRepo;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private JdbcTemplate jdbc;

  @Test
  void persistsTokenMappingWithoutEagerlyLoadingUser() {
    User user = new User();
    user.setEmail("jpa-token-%s@example.com".formatted(UUID.randomUUID()));
    user.setPassword("test-password-hash");
    user.setActive(true);
    user.getRoles().add(Roles.CLIENT);
    userRepo.saveAndFlush(user);

    byte[] tokenHash = new byte[32];
    tokenHash[0] = 42;
    Instant expiresAt = Instant.now()
        .plus(Duration.ofHours(24))
        .truncatedTo(ChronoUnit.MICROS);

    EmailVerificationToken token = tokenRepo.saveAndFlush(
        new EmailVerificationToken(user, tokenHash, expiresAt));
    UUID tokenId = token.getId();

    entityManager.clear();

    EmailVerificationToken reloaded = tokenRepo.findById(tokenId).orElseThrow();
    PersistenceUnitUtil persistence = entityManager.getEntityManagerFactory()
        .getPersistenceUnitUtil();
    assertThat(persistence.isLoaded(reloaded, "user")).isFalse();

    Map<String, Object> stored = jdbc.queryForMap("""
        SELECT user_id, token_hash, expires_at, consumed_at, revoked_at, created_at, updated_at
        FROM email_verification_tokens
        WHERE id = ?
        """, tokenId);

    assertThat(stored.get("user_id")).isEqualTo(user.getId());
    assertThat((byte[]) stored.get("token_hash")).containsExactly(tokenHash);
    assertThat(((Timestamp) stored.get("expires_at")).toInstant()).isEqualTo(expiresAt);
    assertThat(stored.get("consumed_at")).isNull();
    assertThat(stored.get("revoked_at")).isNull();
    assertThat(stored.get("created_at")).isNotNull();
    assertThat(stored.get("updated_at")).isNotNull();
  }
}
