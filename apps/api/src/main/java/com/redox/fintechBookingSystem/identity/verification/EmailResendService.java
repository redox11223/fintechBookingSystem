package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.User;
import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailResendService {
  private final UserRepo userRepo;
  private final EmailVerificationTokenRepo tokenRepo;
  private final IdentityProperties identityProperties;
  private final EmailVerificationTokenGenerator tokenGenerator;
  private final Clock clock;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void resend(String email){
    String normalizedEmail=email.strip().toLowerCase(Locale.ROOT);
    Optional<User> candidate= userRepo.findByEmail(normalizedEmail);
    if(candidate.isEmpty()){
      return; //account doesnt exist
    }
    User user=candidate.get();
    if(user.getEmailVerifiedAt()!=null){
      return; //account already verified
    }
    Optional<EmailVerificationToken> verificationToken=tokenRepo.findOpenTokenByUserId(user.getId());
    Instant now=Instant.now(clock);
    if(verificationToken.isPresent()){
      var tokenCooldownTime=verificationToken.get().getCreatedAt()
              .plus(identityProperties.tokens().resendCooldown());
      if(tokenCooldownTime.isAfter(now)){
        return;//cooldown still active cant generate another request
      }
      verificationToken.get().revokeAt(now);
      tokenRepo.flush();
    }
    GeneratedEmailVerificationToken newVerificationToken=tokenGenerator.generate();
    Instant tokenExpiredTime=now.plus(identityProperties.tokens().emailVerificationTtl());
    EmailVerificationToken newEmailVerificationToken= new EmailVerificationToken(
            user, newVerificationToken.tokenHash(), tokenExpiredTime);
    tokenRepo.save(newEmailVerificationToken);
    eventPublisher.publishEvent(
            new EmailVerificationRequested(user.getEmail(), newVerificationToken.rawToken()));
  }
}
