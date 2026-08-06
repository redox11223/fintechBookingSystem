package com.redox.fintechBookingSystem.advisor;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "time_offs")
public class TimeOff extends BaseEntity {
  // the advisor can be null, the relationship can be optional because of holidays
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "advisor_id")
  private Advisor advisor;

  @Column(name = "off_date",nullable = false)
  private LocalDate offDate;

  private String description;
}
