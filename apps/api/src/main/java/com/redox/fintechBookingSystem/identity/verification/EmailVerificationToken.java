package com.redox.fintechBookingSystem.identity.verification;

import com.redox.fintechBookingSystem.identity.User;
import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public EmailVerificationToken(User user, byte[] tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }
}
