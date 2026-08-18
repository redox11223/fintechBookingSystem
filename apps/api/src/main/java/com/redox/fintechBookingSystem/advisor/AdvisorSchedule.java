package com.redox.fintechBookingSystem.advisor;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "advisor_schedules")
public class AdvisorSchedule extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "advisor_id",nullable = false)
  private Advisor advisor;

  @Column(name = "day_of_week", nullable = false)
  private int dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;
}
