package com.redox.fintechBookingSystem.identity.session;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "session_id",nullable = false)
  private AuthSession session;

  @Column(name = "token_hash",nullable = false)
  @Getter(AccessLevel.NONE)
  private byte[] tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public RefreshToken(AuthSession session,byte[] tokenHash,Instant expiresAt){
    Objects.requireNonNull(session);
    Objects.requireNonNull(expiresAt);
    Objects.requireNonNull(tokenHash);
    if(tokenHash.length!=32){
      throw new IllegalArgumentException("The token hash needs to be exactly 32 bytes");
    }
    if(expiresAt.isAfter(session.getAbsoluteExpiresAt())){
      throw new IllegalArgumentException("The refresh token cant expire after the session");
    }
    this.session=session;
    this.tokenHash=tokenHash.clone();
    this.expiresAt=expiresAt;
  }
  public byte[] getTokenHash(){
    return Arrays.copyOf(tokenHash,tokenHash.length);
  }
  public boolean isOpen(){
    return consumedAt==null && revokedAt==null;
  }
  public void revokeAt(Instant now){
    Objects.requireNonNull(now);
    if(!isOpen()){
      throw new IllegalStateException("Only an open refresh token can be revoked");
    }
    this.revokedAt=now; //only an open token can be revoked

  }

}
