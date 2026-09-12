package com.redox.fintechBookingSystem.identity.mail;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationRequested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationMailListenerTests {
  private static final String RAW_TOKEN = "url_safe-token";

  @Mock private JavaMailSender mailSender;
  @Mock private IdentityProperties properties;

  private EmailVerificationMailListener listener;

  @BeforeEach
  void setUp() {
    listener = new EmailVerificationMailListener(mailSender, properties);
    when(properties.mail()).thenReturn(new IdentityProperties.Mail(
        "no-reply@citafin.local", URI.create("https://app.citafin.dev/")));
  }

  @Test
  void sendsPlainTextVerificationMessageUsingConfiguredOrigin() {
    listener.sendEmail(new EmailVerificationRequested("client@example.com", RAW_TOKEN));

    ArgumentCaptor<SimpleMailMessage> messageCaptor =
        ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    SimpleMailMessage message = messageCaptor.getValue();
    assertThat(message.getFrom()).isEqualTo("no-reply@citafin.local");
    assertThat(message.getTo()).containsExactly("client@example.com");
    assertThat(message.getSubject()).isEqualTo("Verificación de Correo");
    assertThat(message.getText())
        .contains("https://app.citafin.dev/verify-email#token=" + RAW_TOKEN);
  }

  @Test
  void smtpFailureDoesNotEscapeAfterCommittedRegistration() {
    doThrow(new MailSendException("SMTP unavailable"))
        .when(mailSender).send(any(SimpleMailMessage.class));

    assertThatCode(() -> listener.sendEmail(
        new EmailVerificationRequested("client@example.com", RAW_TOKEN)))
        .doesNotThrowAnyException();
  }
}
