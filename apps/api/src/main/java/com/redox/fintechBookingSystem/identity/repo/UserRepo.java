package com.redox.fintechBookingSystem.identity.repo;

import com.redox.fintechBookingSystem.identity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);
}
