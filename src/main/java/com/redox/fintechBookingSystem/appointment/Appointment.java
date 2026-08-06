package com.redox.fintechBookingSystem.appointment;

import com.redox.fintechBookingSystem.advisor.Advisor;
import com.redox.fintechBookingSystem.catalog.FinancialService;
import com.redox.fintechBookingSystem.client.Client;
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
@Table(name = "appointments")
public class Appointment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "client_id",nullable = false)
  private Client client;

  @ManyToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "advisor_id",nullable = false)
  private Advisor advisor;

  @ManyToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "service_id",nullable = false)
  private FinancialService service;

  @Version//optimistic locking
  @Column(nullable = false)
  private Long version;

  @Column(name = "start_time",nullable = false)
  private OffsetDateTime startTime;

  @Column(name = "end_time",nullable = false)
  private OffsetDateTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false,length = 30)
  private AppointmentStatus status;
}
