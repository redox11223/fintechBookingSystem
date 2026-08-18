package com.redox.fintechBookingSystem.advisor;

import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import com.redox.fintechBookingSystem.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "advisors")
public class Advisor extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "user_id",unique = true,nullable = false)
  private User user;

  @Column(name = "full_name",nullable = false,length = 100)
  private String fullName;

  @Column(name = "internal_code",nullable = false,unique = true, length = 20)
  private String internalCode;

  @Column(nullable = false, length = 100)
  private String specialization;
}
