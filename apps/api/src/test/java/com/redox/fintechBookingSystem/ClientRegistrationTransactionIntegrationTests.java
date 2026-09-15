package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.client.ClientRegistrationService;
import com.redox.fintechBookingSystem.client.repo.ClientRepo;
import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import com.redox.fintechBookingSystem.identity.repo.UserRepo;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
  @Autowired private UserRepo userRepo;
  @Autowired private EmailVerificationTokenRepo verificationTokenRepo;
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
}
