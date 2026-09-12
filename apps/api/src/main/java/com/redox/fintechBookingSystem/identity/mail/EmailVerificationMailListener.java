package com.redox.fintechBookingSystem.identity.mail;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;


@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationMailListener {
  private final JavaMailSender mailSender;
  private final IdentityProperties properties;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendEmail(EmailVerificationRequested event){
    var mail=properties.mail();
    String verificationLink=buildVerificationLink(mail.frontendBaseUrl(), event.rawToken());
    SimpleMailMessage mailMessage=new SimpleMailMessage();
    mailMessage.setFrom(mail.from());
    mailMessage.setTo(event.email());
    mailMessage.setSubject("Verificación de Correo");
    mailMessage.setText("Bienvenido. Para verificar tu cuenta ingresa al siguiente enlace:\n\n" +verificationLink);
    try {
      mailSender.send(mailMessage);
    } catch (MailException e) {
      log.warn( "Email verification message could not be sent after registration ({})",
              e.getClass().getSimpleName());
    }

  }

  private String buildVerificationLink(URI frontendUrl, String rawToken){
    return UriComponentsBuilder
            .fromUri(frontendUrl)
            .pathSegment("verify-email")
            .fragment("token="+rawToken)
            .build()
            .toUriString();
  }
}
