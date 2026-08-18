package com.redox.fintechBookingSystem.user;

import com.redox.fintechBookingSystem.advisor.Advisor;
import com.redox.fintechBookingSystem.client.Client;
import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {
  @OneToOne(mappedBy = "user",cascade = CascadeType.ALL)
  private Client client;

  @OneToOne(mappedBy = "user",cascade = CascadeType.ALL)
  private Advisor advisor;

  @Column(nullable = false,unique = true,length = 150)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String password;

  @Enumerated(value = EnumType.STRING)
  @Column(nullable = false,length = 50)
  private Roles role;

  @Column(name = "is_active")
  private boolean isActive=true;
}
