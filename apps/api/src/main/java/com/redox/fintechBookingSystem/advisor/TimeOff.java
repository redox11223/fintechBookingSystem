package com.redox.fintechBookingSystem.advisor;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

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

  @Column(name = "starts_at",nullable = false)
  private OffsetDateTime startsAt;

  @Column(name = "ends_at",nullable = false)
  private OffsetDateTime endsAt;

  @Column(nullable = false, length = 500)
  private String reason;
}
