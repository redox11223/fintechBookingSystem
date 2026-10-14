package com.redox.fintechBookingSystem.identity.session;

import com.redox.fintechBookingSystem.identity.authentication.AuthenticatedResult;
import com.redox.fintechBookingSystem.identity.authentication.jwt.AccessTokenIssuer;
import com.redox.fintechBookingSystem.identity.authentication.jwt.IssuedAccessToken;
import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RefreshService {
  private final RefreshTransaction refreshTransaction;
  private final AccessTokenIssuer accessTokenIssuer;
  private final IdentityProperties properties;

  public AuthenticatedResult refresh(String rawRefreshToken){
     RefreshOutcome refreshOutcome=refreshTransaction.refresh(rawRefreshToken);
     if(refreshOutcome instanceof RefreshOutcome.Failure){
       throw new InvalidRefreshSessionException();
     }
     RefreshOutcome.Success success=(RefreshOutcome.Success) refreshOutcome;
    IssuedAccessToken accessToken=accessTokenIssuer.issue(
            success.userId(),
            success.sessionId(),
            success.roles()
    );
    return new AuthenticatedResult(
            accessToken,
            success.refreshToken(),
            properties.session().accessTokenTtl().getSeconds(),
            success.absoluteExpiresAt()
    );
  }
}
