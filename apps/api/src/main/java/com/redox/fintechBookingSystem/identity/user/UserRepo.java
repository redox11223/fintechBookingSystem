package com.redox.fintechBookingSystem.identity.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<User> findByEmail(String email);
}
