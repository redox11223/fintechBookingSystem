package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.client.ClientRegistrationService;
import com.redox.fintechBookingSystem.client.repo.ClientRepo;
import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import com.redox.fintechBookingSystem.identity.user.UserRepo;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "citafin.identity.password.bcrypt-strength=10",
    "management.health.mail.enabled=false"
})
@ActiveProfiles("test")
class ClientRegistrationTransactionIntegrationTests {
  private static final String EMAIL = "rollback-client@example.com";

  @Autowired private ClientRegistrationService registrationService;
  @Autowired private ClientRepo clientRepo;
  @MockitoSpyBean private UserRepo userRepo;
  @Autowired private EmailVerificationTokenRepo verificationTokenRepo;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockitoBean private JavaMailSender mailSender;

  @Test
  void rollsBackIdentityAndTokenWhenClientPersistenceFails() {
    long clientsBefore = clientRepo.count();
    long tokensBefore = verificationTokenRepo.count();
    ClientRegistrationRequest request = new ClientRegistrationRequest(
        "A".repeat(121),
        EMAIL,
        "a sufficiently long password",
        "+51987654321");

    assertThatThrownBy(() -> registrationService.registerClient(request))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(userRepo.existsByEmail(EMAIL)).isFalse();
    assertThat(clientRepo.count()).isEqualTo(clientsBefore);
    assertThat(verificationTokenRepo.count()).isEqualTo(tokensBefore);
    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  void sendsVerificationEmailAfterSuccessfulRegistrationCommit() {
    String committedEmail = "committed-client@example.com";
    ClientRegistrationRequest request = new ClientRegistrationRequest(
        "María Quispe",
        committedEmail,
        "a sufficiently long password",
        "+51987654321");

    registrationService.registerClient(request);

    assertThat(userRepo.existsByEmail(committedEmail)).isTrue();
    verify(mailSender).send(any(SimpleMailMessage.class));
  }

  @Test
  void concurrentDuplicateEmailRegistrationsCreateOneAccountAndDoNotThrow() throws Exception {
    String duplicateEmail = "duplicate-client@example.com";
    CyclicBarrier bothPrevalidationsCompleted = new CyclicBarrier(2);
    AtomicInteger matchingEmailChecks = new AtomicInteger();

    doAnswer(invocation -> {
      String checkedEmail = invocation.getArgument(0);
      Boolean exists = jdbcTemplate.queryForObject(
          "select exists(select 1 from users where lower(email) = lower(?))",
          Boolean.class,
          checkedEmail);
      int checkNumber = matchingEmailChecks.incrementAndGet();
      if (checkNumber <= 2) {
        bothPrevalidationsCompleted.await(10, TimeUnit.SECONDS);
      }
      return Boolean.TRUE.equals(exists);
    }).when(userRepo).existsByEmail(duplicateEmail);

    ClientRegistrationRequest firstRequest = new ClientRegistrationRequest(
        "María Quispe",
        duplicateEmail,
        "a sufficiently long password",
        "+51987654321");
    ClientRegistrationRequest secondRequest = new ClientRegistrationRequest(
        "Juan Pérez",
        duplicateEmail,
        "another sufficiently long password",
        "+51987654322");

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> firstRegistration = executor.submit(
          () -> registrationService.registerClient(firstRequest));
      Future<?> secondRegistration = executor.submit(
          () -> registrationService.registerClient(secondRequest));

      assertThatCode(() -> {
        firstRegistration.get(15, TimeUnit.SECONDS);
        secondRegistration.get(15, TimeUnit.SECONDS);
      }).doesNotThrowAnyException();
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(matchingEmailChecks).hasValue(2);
    assertThat(countRows(
        "select count(*) from users where lower(email) = lower(?)", duplicateEmail))
        .isEqualTo(1);
    assertThat(countRows("""
        select count(*)
        from user_roles r
        join users u on u.id = r.user_id
        where lower(u.email) = lower(?) and r.role = 'CLIENT'
        """, duplicateEmail)).isEqualTo(1);
    assertThat(countRows("""
        select count(*)
        from clients c
        join users u on u.id = c.user_id
        where lower(u.email) = lower(?)
        """, duplicateEmail)).isEqualTo(1);
    assertThat(countRows("""
        select count(*)
        from email_verification_tokens t
        join users u on u.id = t.user_id
        where lower(u.email) = lower(?)
        """, duplicateEmail)).isEqualTo(1);
    verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
  }

  private long countRows(String sql, String email) {
    Long count = jdbcTemplate.queryForObject(sql, Long.class, email);
    return count == null ? 0 : count;
  }
}
