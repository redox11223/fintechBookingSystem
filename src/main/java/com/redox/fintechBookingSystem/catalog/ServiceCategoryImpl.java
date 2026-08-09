package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryRequest;
import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryResponse;
import com.redox.fintechBookingSystem.catalog.mapper.ServiceCategoryMapper;
import com.redox.fintechBookingSystem.shared.exception.DuplicateResourceException;
import com.redox.fintechBookingSystem.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCategoryImpl implements ServiceCategoryService{

  private final ServiceCategoryRepo categoryRepo;
  private final ServiceCategoryMapper categoryMapper;

  @Transactional
  @Override
  public ServiceCategoryResponse registerCategory(ServiceCategoryRequest request) {
    if (categoryRepo.existsByNameIgnoreCase(request.name())){
      throw new DuplicateResourceException("Category already exists");
    }
    ServiceCategory serviceCategory=new ServiceCategory(request.name(), request.description());
    categoryRepo.save(serviceCategory);
    return categoryMapper.serviceCategoryToDto(serviceCategory);
  }

  @Transactional
  @Override
  public ServiceCategoryResponse updateCategory(UUID id, ServiceCategoryRequest request) {
    if(categoryRepo.existsByNameIgnoreCaseAndIdNot(request.name(),id)){
      throw new DuplicateResourceException("Category already exists");
    }
    ServiceCategory serviceCategory=categoryRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
    serviceCategory.setName(request.name());
    serviceCategory.setDescription(request.description());
    return categoryMapper.serviceCategoryToDto(serviceCategory);
  }

  @Override
  public ServiceCategoryResponse getCategory(UUID id) {
    ServiceCategory serviceCategory=categoryRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
    return categoryMapper.serviceCategoryToDto(serviceCategory);
  }
}
