package com.redox.fintechBookingSystem.identity.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByTokenHash(byte[] tokenHash);
}
