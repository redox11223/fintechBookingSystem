package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceRequest;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceResponse;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceUpdate;

import java.util.UUID;

public interface FinancialServiceService {
  FinancialServiceResponse registerFinancialService(FinancialServiceRequest serviceRequest);
  FinancialServiceResponse updateFinancialService(UUID id, FinancialServiceUpdate serviceUpdate);
  FinancialServiceResponse getFinancialService(UUID id);
  FinancialService getFinancialServiceEntity(UUID id);
}
