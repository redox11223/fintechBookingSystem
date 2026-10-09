package com.redox.fintechBookingSystem.identity.authentication;

import com.redox.fintechBookingSystem.identity.authentication.exception.AccountDisabledException;
import com.redox.fintechBookingSystem.identity.authentication.exception.AuthenticationFailedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.EmailNotVerifiedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.MfaRequiredException;
import com.redox.fintechBookingSystem.identity.authentication.jwt.AccessTokenIssuer;
import com.redox.fintechBookingSystem.identity.authentication.jwt.IssuedAccessToken;
import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
  private final AccessTokenIssuer tokenIssuer;
  private final LoginTransaction loginTransaction;
  private final IdentityProperties properties;

  public AuthenticatedLogin login(String email,String password){
    LoginOutcome loginOutcome=loginTransaction.attempt(email, password);
    if(loginOutcome instanceof LoginOutcome.Success success){
      IssuedAccessToken accessToken= tokenIssuer.issue(
              success.userId(),
              success.sessionId(),
              success.roles()
      );
      return new AuthenticatedLogin(
              accessToken,
              success.refreshToken(),
              properties.session().accessTokenTtl().getSeconds());
    }
    LoginOutcome.Failure failure=(LoginOutcome.Failure) loginOutcome;
    throw switch (failure)
    {
      case INVALID_CREDENTIALS -> new AuthenticationFailedException();
      case ACCOUNT_DISABLED -> new AccountDisabledException();
      case EMAIL_NOT_VERIFIED -> new EmailNotVerifiedException();
      case MFA_REQUIRED -> new MfaRequiredException();
    };
  }
}
