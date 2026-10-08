package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.user.User;
import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

//internal entity to store email verification tokens.
//The token itself is not stored, only its hash for security reasons.
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash",nullable = false)
  private byte[] tokenHash;

  @Column(name = "expires_at",nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  @Setter(AccessLevel.NONE)
  @Column(name = "revoked_at")
  private Instant revokedAt;

  public EmailVerificationToken(User user, byte[] tokenHash, Instant expiresAt) {
    Objects.requireNonNull(user);
    Objects.requireNonNull(tokenHash);
    Objects.requireNonNull(expiresAt);
    if(tokenHash.length!=32){
      throw new IllegalArgumentException("The token hash needs to be exactly 32 bytes");
    }
    this.user = user;
    this.tokenHash = tokenHash.clone(); // Defensive copy to prevent external modification
    this.expiresAt = expiresAt;
  }

  public void revokeAt(Instant revokedTime){
    Objects.requireNonNull(revokedTime,"Revocation time is required");
    if(revokedAt!=null || consumedAt!=null){
      throw new IllegalStateException("Only an open token can be revoked");
    }
    this.revokedAt=revokedTime;
  }
}
