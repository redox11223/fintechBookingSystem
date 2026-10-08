package com.redox.fintechBookingSystem.identity.user;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {
  @Column(nullable = false,length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String password;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(value = EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  private Set<Roles> roles = new HashSet<>();

  @Column(name = "email_verified_at")
  private Instant emailVerifiedAt;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "failed_login_count",nullable = false)
  private int failedLoginCount;

  @Column(name = "failure_window_started_at")
  private Instant failureWindowStartedAt;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  public User(String email,String password){
    this.email=email;
    this.password=password;
    this.emailVerifiedAt=null;
    this.isActive=true;
  }

  public void addRole(Roles role){
    Objects.requireNonNull(role);
    roles.add(role);
  }
  public boolean hasAnyRole(Roles... rolesToCheck){
    return Arrays.stream(rolesToCheck).anyMatch(role->roles.contains(role));
  }

  public void verifyEmailAt(Instant time){
    Objects.requireNonNull(time);
    this.emailVerifiedAt=time;
  }

  public boolean isLoginLockedAt(Instant now){
    Objects.requireNonNull(now);
    return lockedUntil !=null && now.isBefore(lockedUntil);
  }

  public void clearLoginFailures(){
    this.failedLoginCount=0;
    this.lockedUntil =null;
    this.failureWindowStartedAt=null;
  }

  public void recordFailedLogin(Instant now, int maxFailures, Duration failureWindow,Duration lockDuration){
    Objects.requireNonNull(now);
    Objects.requireNonNull(failureWindow);
    Objects.requireNonNull(lockDuration);
    if(maxFailures<1){
      throw new IllegalArgumentException("The number of failures cant be lower than 1");
    }
    if (failureWindow.isNegative() || failureWindow.isZero()){
      throw new IllegalArgumentException("The failure window duration can't be negative or zero");
    }
    if (lockDuration.isNegative() || lockDuration.isZero()){
      throw new IllegalArgumentException("The lock duration can't be negative or zero");
    }
    if(isLoginLockedAt(now)){
      return;
    }
    Instant windowExpiresAt=this.failureWindowStartedAt==null?null:this.failureWindowStartedAt.plus(failureWindow);
    boolean isWindowClosed=this.failureWindowStartedAt==null || !now.isBefore(windowExpiresAt);
    if(isWindowClosed){
      //open the window again
      this.failureWindowStartedAt=now;
      this.failedLoginCount=1;
      this.lockedUntil=null;
    }else{
      failedLoginCount++;
    }
    if(this.failedLoginCount>=maxFailures){
      this.lockedUntil =now.plus(lockDuration);
    }

  }
}
