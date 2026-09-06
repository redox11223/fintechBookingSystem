package com.redox.fintechBookingSystem.identity.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailVerificationTokenRepo extends JpaRepository<EmailVerificationToken, UUID> {
}
