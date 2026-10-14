package com.redox.fintechBookingSystem.identity.session;

import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.user.Roles;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public sealed interface RefreshOutcome permits  RefreshOutcome.Success,RefreshOutcome.Failure{
  record Success(
          UUID userId,
          UUID sessionId,
          Set<Roles> roles,
          GeneratedSecureToken refreshToken,
          Instant absoluteExpiresAt
  )implements RefreshOutcome{
    public Success{
      Objects.requireNonNull(userId, "User ID is required");
      Objects.requireNonNull(sessionId, "Session ID is required");
      Objects.requireNonNull(roles, "Roles are required");
      Objects.requireNonNull(refreshToken, "Refresh token is required");
      Objects.requireNonNull(absoluteExpiresAt, "Absolute expire time is required");
      if(roles.isEmpty()){
        throw new IllegalArgumentException("At least one role is required for a successful refresh");
      }
      roles=Set.copyOf(roles);
    }
  }

  enum Failure implements RefreshOutcome{
    INVALID_SESSION
  }
}
