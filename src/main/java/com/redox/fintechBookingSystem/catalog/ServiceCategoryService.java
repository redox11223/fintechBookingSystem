package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryRequest;
import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryResponse;

import java.util.UUID;

public interface ServiceCategoryService {
  ServiceCategoryResponse registerCategory(ServiceCategoryRequest request);
  ServiceCategoryResponse updateCategory(UUID id,ServiceCategoryRequest request);
  ServiceCategoryResponse getCategory(UUID id);
}
