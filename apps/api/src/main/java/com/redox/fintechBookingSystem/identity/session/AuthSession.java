package com.redox.fintechBookingSystem.identity.session;

import com.redox.fintechBookingSystem.identity.user.User;
import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "auth_sessions")
public class AuthSession extends BaseEntity {
  //optional=false indicates that the relationship should always exist
  //or to optimize the query if the type of fetch is Eager(hibernate will use Inner Joins)
  @ManyToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "user_id",nullable = false)
  private User user;

  @Column(name = "absolute_expires_at", nullable = false)
  private Instant absoluteExpiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public AuthSession(User user,Instant absoluteExpiresAt){
    Objects.requireNonNull(user);
    Objects.requireNonNull(absoluteExpiresAt);
    this.user=user;
    this.absoluteExpiresAt=absoluteExpiresAt;
  }

  public boolean isActiveAt(Instant now){
    Objects.requireNonNull(now);
    return revokedAt==null && now.isBefore(absoluteExpiresAt);
  }

  public void revokeAt(Instant now){
    Objects.requireNonNull(now);
    if(revokedAt!=null){
      throw new IllegalStateException("The session is already revoked");
    }
    this.revokedAt=now;

  }
}
