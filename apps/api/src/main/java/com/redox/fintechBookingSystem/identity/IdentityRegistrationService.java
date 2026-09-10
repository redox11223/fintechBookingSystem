package com.redox.fintechBookingSystem.identity;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.password.PasswordPolicy;
import com.redox.fintechBookingSystem.identity.repo.UserRepo;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationToken;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenGenerator;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationTokenRepo;
import com.redox.fintechBookingSystem.identity.verification.GeneratedEmailVerificationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IdentityRegistrationService {
  private final UserRepo userRepo;
  private final PasswordPolicy passwordPolicy;
  private final EmailVerificationTokenGenerator tokenGenerator;
  private final PasswordEncoder passwordEncoder;
  private final IdentityProperties identityProperties;
  private final EmailVerificationTokenRepo verificationTokenRepo;

  @Transactional
  public Optional<User> registerPendingUser(String email, String password){
    String formattedEmail=email.strip().toLowerCase(Locale.ROOT);
    if(userRepo.existsByEmail(formattedEmail)){
      throw new RuntimeException("Invalid Credentials");
    }
    String formattedPassword=passwordPolicy.validateAndNormalize(password);
    String hashedPassword= passwordEncoder.encode(formattedPassword);
    User newUser=new User();
    newUser.setEmail(formattedEmail);
    newUser.setPassword(hashedPassword);
    newUser.setRoles(Set.of(Roles.CLIENT));
    newUser.setEmailVerifiedAt(null);
    newUser.setActive(true);
    User savedUser= userRepo.save(newUser);
    GeneratedEmailVerificationToken verificationToken=tokenGenerator.generate();
    Instant tokenExpiration=Instant.now().plus(identityProperties.tokens().emailVerificationTtl());
    EmailVerificationToken emailVerificationToken=new EmailVerificationToken(
            savedUser,verificationToken.tokenHash(), tokenExpiration);
    verificationTokenRepo.save(emailVerificationToken);
    return Optional.of(newUser);
  }
}
