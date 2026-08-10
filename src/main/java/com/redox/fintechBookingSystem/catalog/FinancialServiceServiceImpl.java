package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceRequest;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceResponse;
import com.redox.fintechBookingSystem.catalog.mapper.FinancialServiceMapper;
import com.redox.fintechBookingSystem.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialServiceServiceImpl implements FinancialServiceService{
  private final FinancialServiceRepo financialServiceRepo;
  private final FinancialServiceMapper financialServiceMapper;

  @Override
  public FinancialServiceResponse registerFinancialService(FinancialServiceRequest serviceRequest) {
    return null;
  }

  @Override
  public FinancialServiceResponse updateFinancialService(UUID id, FinancialServiceRequest serviceRequest) {
    return null;
  }

  @Override
  public FinancialServiceResponse getFinancialService(UUID id) {
    FinancialService service= financialServiceRepo.findById(id).
            orElseThrow(()->new ResourceNotFoundException("Financial Service not found"));
    return financialServiceMapper.financialServiceToDto(service);
  }
}
