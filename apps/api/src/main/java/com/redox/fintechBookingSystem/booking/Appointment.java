package com.redox.fintechBookingSystem.booking;

import com.redox.fintechBookingSystem.advisor.Advisor;
import com.redox.fintechBookingSystem.catalog.FinancialService;
import com.redox.fintechBookingSystem.customer.Client;
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

  @Column(name = "starts_at",nullable = false)
  private OffsetDateTime startsAt;

  @Column(name = "ends_at",nullable = false)
  private OffsetDateTime endsAt;

  @Column(name = "blocked_until",nullable = false)
  private OffsetDateTime blockedUntil;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(name = "client_comment", length = 1000)
  private String clientComment;

  @Column(name = "consented_at", nullable = false)
  private OffsetDateTime consentedAt;

  @Column(name = "consent_version", nullable = false, length = 30)
  private String consentVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false,length = 30)
  private AppointmentStatus status;
}
