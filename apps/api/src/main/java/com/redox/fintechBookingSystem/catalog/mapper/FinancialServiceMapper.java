package com.redox.fintechBookingSystem.catalog.mapper;

import com.redox.fintechBookingSystem.catalog.FinancialService;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FinancialServiceMapper {
  @Mapping(source = "category.name",target = "categoryName")
  FinancialServiceResponse financialServiceToDto(FinancialService financialService);
}
