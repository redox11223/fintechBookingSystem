package com.redox.fintechBookingSystem.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceCategoryRepo extends JpaRepository<ServiceCategory, UUID> {
  boolean existsByNameIgnoreCase(String name);
  boolean existsByNameIgnoreCaseAndIdNot(String name,UUID id);
}
