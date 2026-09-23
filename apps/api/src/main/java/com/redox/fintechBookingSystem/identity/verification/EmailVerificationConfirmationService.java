package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.User;
import com.redox.fintechBookingSystem.identity.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationConfirmationService {
  private final EmailVerificationTokenGenerator tokenGenerator;
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
