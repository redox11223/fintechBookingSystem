package com.redox.fintechBookingSystem.client.repo;

import com.redox.fintechBookingSystem.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepo extends JpaRepository<Client, UUID> {
}
