package com.redox.fintechBookingSystem.client;

import com.redox.fintechBookingSystem.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
