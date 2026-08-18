package com.redox.fintechBookingSystem.user;

import com.redox.fintechBookingSystem.advisor.Advisor;
import com.redox.fintechBookingSystem.client.Client;
import com.redox.fintechBookingSystem.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {
  @OneToOne(mappedBy = "user")
  private Client client;

  @OneToOne(mappedBy = "user")
  private Advisor advisor;

  @Column(nullable = false,length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String password;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(value = EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  private Set<Roles> roles = new HashSet<>();

  @Column(name = "email_verified_at")
  private OffsetDateTime emailVerifiedAt;

  @Column(name = "is_active", nullable = false)
  private boolean isActive=true;
}
