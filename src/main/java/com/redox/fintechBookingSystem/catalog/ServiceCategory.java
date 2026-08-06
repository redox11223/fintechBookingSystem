package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_categories")
public class ServiceCategory extends BaseEntity {
  @Column(nullable = false,length = 100,unique = true)
  private String name;
  private String description;
}
