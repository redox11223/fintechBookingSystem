package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceRequest;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceResponse;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceUpdate;
import com.redox.fintechBookingSystem.catalog.mapper.FinancialServiceMapper;
import com.redox.fintechBookingSystem.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialServiceServiceImpl implements FinancialServiceService{
  private final FinancialServiceRepo financialServiceRepo;
  private final FinancialServiceMapper financialServiceMapper;
  private final ServiceCategoryService categoryService;

  @Transactional
  @Override
  public FinancialServiceResponse registerFinancialService(FinancialServiceRequest serviceRequest) {
    ServiceCategory category=categoryService.getCategoryEntity(serviceRequest.categoryId());
    FinancialService financialService= FinancialService.builder()
            .category(category)
            .name(serviceRequest.name())
            .durationMinutes(serviceRequest.durationMinutes())
            .build();
    return financialServiceMapper.financialServiceToDto(financialServiceRepo.save(financialService));
  }

  @Transactional
  @Override
  public FinancialServiceResponse updateFinancialService(UUID id, FinancialServiceUpdate serviceUpdate) {
    FinancialService service= getFinancialServiceEntity(id);
    ServiceCategory category=categoryService.getCategoryEntity(serviceUpdate.categoryId());
    service.setCategory(category);
    service.setName(serviceUpdate.name());
    service.setDurationMinutes(serviceUpdate.durationMinutes());
    service.setActive(serviceUpdate.isActive());
    financialServiceRepo.save(service);
    return financialServiceMapper.financialServiceToDto(service);
  }
  @Transactional(readOnly = true)
  @Override
  public FinancialServiceResponse getFinancialService(UUID id) {
    FinancialService service= getFinancialServiceEntity(id);
    return financialServiceMapper.financialServiceToDto(service);
  }
  @Transactional(propagation = Propagation.MANDATORY)
  @Override
  public FinancialService getFinancialServiceEntity(UUID id){
    return financialServiceRepo.findById(id).
            orElseThrow(()->new ResourceNotFoundException("Financial Service not found"));
  }
}
