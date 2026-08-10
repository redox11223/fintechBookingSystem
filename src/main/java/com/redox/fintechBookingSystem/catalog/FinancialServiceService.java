package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceRequest;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceResponse;
import java.util.UUID;

public interface FinancialServiceService {
  FinancialServiceResponse registerFinancialService(FinancialServiceRequest serviceRequest);
  FinancialServiceResponse updateFinancialService(UUID id, FinancialServiceRequest serviceRequest);
  FinancialServiceResponse getFinancialService(UUID id);
}
