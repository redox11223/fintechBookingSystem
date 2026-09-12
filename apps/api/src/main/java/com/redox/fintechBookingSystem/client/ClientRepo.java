package com.redox.fintechBookingSystem.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepo extends JpaRepository<Client, UUID> {
}
