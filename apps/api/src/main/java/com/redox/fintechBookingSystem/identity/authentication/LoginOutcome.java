package com.redox.fintechBookingSystem.identity.authentication;

import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.user.Roles;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * this is an example of a Result Pattern
 * Internal result of a login attempt. Failures are returned instead of thrown so transactional
 * updates such as failed-login counters can commit before the public error is produced.
 * you can declare a class,record,enum inside an interface,and they are public and static by default
 */
public sealed interface LoginOutcome permits LoginOutcome.Success,LoginOutcome.Failure{
  record Success(
          UUID userId,
          UUID sessionId,
          Set<Roles> roles,
          GeneratedSecureToken refreshToken
  ) implements LoginOutcome{
    public Success{
      Objects.requireNonNull(userId, "User ID is required");
      Objects.requireNonNull(sessionId, "Session ID is required");
      Objects.requireNonNull(roles, "Roles are required");
      Objects.requireNonNull(refreshToken, "Refresh token is required");
      if(roles.isEmpty()){
        throw new IllegalArgumentException("At least one role is required for a successful login");
      }
      roles=Set.copyOf(roles);
    }
  }

  enum Failure implements LoginOutcome{
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    EMAIL_NOT_VERIFIED,
    MFA_REQUIRED
  }
}
