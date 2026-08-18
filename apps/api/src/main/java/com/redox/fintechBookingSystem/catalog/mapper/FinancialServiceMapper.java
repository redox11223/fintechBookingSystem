package com.redox.fintechBookingSystem.catalog.mapper;

import com.redox.fintechBookingSystem.catalog.FinancialService;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FinancialServiceMapper {
  @Mapping(source = "category.name",target = "categoryName")
  @Mapping(source = "active",target = "isActive")
  FinancialServiceResponse financialServiceToDto(FinancialService financialService);
}
