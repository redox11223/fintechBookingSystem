package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "financial_services")
public class FinancialService extends BaseEntity {
  // Optional = false tells JPA that a service CAN'T exist without a category,this
  // option is only available in oneToOne and ManyToOne relationships
  @ManyToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "category_id",nullable = false)
  private ServiceCategory category;

  @Column(nullable = false,length = 100)
  private String name;

  private int durationMinutes;

  @Column(name = "is_active")
  private boolean isActive=true;
}
