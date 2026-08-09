package com.redox.fintechBookingSystem.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FinancialServiceRepo extends JpaRepository<FinancialService, UUID> {
}
