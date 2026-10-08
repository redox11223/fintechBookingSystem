package com.redox.fintechBookingSystem.identity.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {
}
