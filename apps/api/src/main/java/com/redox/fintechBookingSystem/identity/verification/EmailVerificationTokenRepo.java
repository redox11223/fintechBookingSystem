package com.redox.fintechBookingSystem.identity.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepo extends JpaRepository<EmailVerificationToken, UUID> {
  @Query("""
SELECT t from EmailVerificationToken t
 WHERE t.user.id = :userId AND
 consumedAt is NULL AND revokedAt iS NULL""")
  Optional<EmailVerificationToken> findOpenTokenByUserId(@Param("userId") UUID userId);
}
