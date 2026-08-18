package com.redox.fintechBookingSystem.catalog.mapper;

import com.redox.fintechBookingSystem.catalog.ServiceCategory;
import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceCategoryMapper {
  ServiceCategoryResponse serviceCategoryToDto(ServiceCategory serviceCategory);
}
