package com.redox.fintechBookingSystem.identity.authentication;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.password.InvalidPasswordException;
import com.redox.fintechBookingSystem.identity.password.PasswordPolicy;
import com.redox.fintechBookingSystem.identity.session.AuthSession;
import com.redox.fintechBookingSystem.identity.session.AuthSessionRepo;
import com.redox.fintechBookingSystem.identity.session.RefreshToken;
import com.redox.fintechBookingSystem.identity.session.RefreshTokenRepo;
import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.identity.token.SecureTokenGenerator;
import com.redox.fintechBookingSystem.identity.user.Roles;
import com.redox.fintechBookingSystem.identity.user.User;
import com.redox.fintechBookingSystem.identity.user.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Component
public class LoginTransaction {
  private static final String DUMMY_RAW_PASSWORD = "this-is-a-dummy-password-only-for-timing";

  private final PasswordEncoder passwordEncoder;
  private final String dummyPasswordHash;
  private final UserRepo userRepo;
  private final PasswordPolicy passwordPolicy;
  private final IdentityProperties identityProperties;
  private final SecureTokenGenerator tokenGenerator;
  private final AuthSessionRepo authSessionRepo;
  private final RefreshTokenRepo tokenRepo;
  private final Clock clock;

  public LoginTransaction(PasswordEncoder passwordEncoder, UserRepo userRepo, PasswordPolicy passwordPolicy, IdentityProperties identityProperties, SecureTokenGenerator tokenGenerator, AuthSessionRepo authSessionRepo, RefreshTokenRepo tokenRepo, Clock clock){
    this.userRepo = userRepo;
    this.passwordPolicy = passwordPolicy;
    this.identityProperties = identityProperties;
    this.tokenGenerator = tokenGenerator;
    this.authSessionRepo = authSessionRepo;
    this.tokenRepo = tokenRepo;
    this.passwordEncoder=passwordEncoder;
    this.dummyPasswordHash=passwordEncoder.encode(DUMMY_RAW_PASSWORD);
    this.clock = clock;
  }

  @Transactional
  public LoginOutcome attempt(String email,String password){
    String normalizedEmail=email.strip().toLowerCase(Locale.ROOT);
    String normalizedPassword;
    try{
       normalizedPassword=passwordPolicy.validateAndNormalize(password);
    }catch (InvalidPasswordException e){
      passwordEncoder.matches(DUMMY_RAW_PASSWORD,dummyPasswordHash);
      return LoginOutcome.Failure.INVALID_CREDENTIALS;
    }
    Optional<User> userContainer=userRepo.findByEmail(normalizedEmail);
    if(userContainer.isEmpty()){
      passwordEncoder.matches(normalizedPassword,dummyPasswordHash);
      return LoginOutcome.Failure.INVALID_CREDENTIALS;
    }
    User user=userContainer.get();
    boolean passwordMatches=passwordEncoder.matches(normalizedPassword,user.getPassword());
    Instant now=Instant.now(clock);
    if(user.isLoginLockedAt(now)){
      return LoginOutcome.Failure.INVALID_CREDENTIALS;
    }
    if(!passwordMatches){
      user.recordFailedLogin(
              now,
              identityProperties.login().maxFailures(),
              identityProperties.login().failureWindow(),
              identityProperties.login().lockDuration()
      );
      return LoginOutcome.Failure.INVALID_CREDENTIALS;
    }
    user.clearLoginFailures();
    if(!user.isActive()){
      return LoginOutcome.Failure.ACCOUNT_DISABLED;
    }
    if(user.getEmailVerifiedAt()==null){
      return LoginOutcome.Failure.EMAIL_NOT_VERIFIED;
    }
    if(user.hasAnyRole(Roles.ADMIN,Roles.ADVISOR)){
      return LoginOutcome.Failure.MFA_REQUIRED;
    }
    Instant absoluteExpiration=now.plus(identityProperties.session().refreshTokenTtl());
    AuthSession authSession=authSessionRepo.save(new AuthSession(user,absoluteExpiration)) ;

    GeneratedSecureToken secureToken=tokenGenerator.generate();
    RefreshToken refreshToken=new RefreshToken(
            authSession, secureToken.tokenHash(),absoluteExpiration);
    tokenRepo.save(refreshToken);

    return new LoginOutcome.Success(
            user.getId(),
            authSession.getId(),
            user.getRoles(),
            secureToken
    );
  }
}
