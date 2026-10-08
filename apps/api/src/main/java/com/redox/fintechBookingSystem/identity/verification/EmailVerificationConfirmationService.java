package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import com.redox.fintechBookingSystem.identity.user.User;
import com.redox.fintechBookingSystem.identity.user.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationConfirmationService {
  private final SecureTokenGenerator tokenGenerator;
  private final EmailVerificationTokenConsumptionRepo tokenConsumptionRepo;
  private final UserRepo userRepo;
  private final Clock clock;

  @Transactional
  public void confirm(String token){
    byte[] requestTokenHash=tokenGenerator.hash(token);
    Instant now= Instant.now(clock);
    UUID userId=tokenConsumptionRepo.consumeIfValid(requestTokenHash, now)
            .orElseThrow(InvalidEmailVerificationTokenException::new);
    User user=userRepo.findById(userId)
            .orElseThrow(InvalidEmailVerificationTokenException::new);
    user.verifyEmailAt(now);
  }
}
