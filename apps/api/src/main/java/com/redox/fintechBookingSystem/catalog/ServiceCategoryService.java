package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryRequest;
import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceCategoryService {
  ServiceCategoryResponse registerCategory(ServiceCategoryRequest request);
  ServiceCategoryResponse updateCategory(UUID id,ServiceCategoryRequest request);
  ServiceCategoryResponse getCategory(UUID id);
  ServiceCategory getCategoryEntity(UUID id);
  Page<ServiceCategoryResponse> getCategories(Pageable pageable);
}
