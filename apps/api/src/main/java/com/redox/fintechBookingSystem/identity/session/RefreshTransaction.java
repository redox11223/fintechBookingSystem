package com.redox.fintechBookingSystem.identity.session;

import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import com.redox.fintechBookingSystem.identity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTransaction {
  private final SecureTokenGenerator tokenGenerator;
  private final AuthSessionRepo authSessionRepo;
  private final RefreshTokenRepo refreshTokenRepo;
  private final Clock clock;

  @Transactional
  public RefreshOutcome refresh(String rawRefreshToken){
    if(rawRefreshToken==null || rawRefreshToken.isBlank()){
      return RefreshOutcome.Failure.INVALID_SESSION;
    }
    byte[] tokenHash=tokenGenerator.hash(rawRefreshToken);

    AuthSession authSession=authSessionRepo.findByTokenHashForUpdate(tokenHash)
            .orElse(null);
    if(authSession==null){
      return RefreshOutcome.Failure.INVALID_SESSION;
    }
    RefreshToken refreshToken=refreshTokenRepo.findByTokenHash(tokenHash)
            .orElse(null);
    if(refreshToken==null){
      return RefreshOutcome.Failure.INVALID_SESSION;
    }
    // A request may wait for the session lock until after its expiration.
    Instant now=Instant.now(clock);
    if(!authSession.isActiveAt(now)){
      return RefreshOutcome.Failure.INVALID_SESSION;
    }
    if(refreshToken.isConsumed()){
      // The consumed token is already unusable, but its replacement may be compromised.
      // Revoke their shared session and return a failure so this write commits before
      // RefreshService throws the public authentication error.
      authSession.revokeAt(now);
      return RefreshOutcome.Failure.INVALID_SESSION;
    }
    if(!refreshToken.isValidAt(now)){
      return RefreshOutcome.Failure.INVALID_SESSION;
    }
    refreshToken.consumeAt(now);
    // Release the unique open-token slot before Hibernate inserts the replacement.
    // Flush is not a commit: an insertion failure still rolls back the consumption.
    refreshTokenRepo.flush();
    GeneratedSecureToken secureToken=tokenGenerator.generate();
    RefreshToken newRefreshToken=new RefreshToken(
            authSession,
            secureToken.tokenHash(),
            authSession.getAbsoluteExpiresAt()
            );
    refreshTokenRepo.save(newRefreshToken);
    User user=authSession.getUser();
    return new RefreshOutcome.Success(
            user.getId(),
            authSession.getId(),
            user.getRoles(),
            secureToken,
            authSession.getAbsoluteExpiresAt()
    );
  }
}
