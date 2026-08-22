package com.redox.fintechBookingSystem.customer;

import com.redox.fintechBookingSystem.identity.User;
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
@Table(name = "clients")
public class Client extends BaseEntity {
  //in oneToOne and ManyToOne relationships the default is eager change it to lazy,joinColumn should
  //be in the entity that has the foreign key
  @OneToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "user_id",nullable = false,unique = true)
  private User user;

  @Column(name = "full_name",nullable = false,length = 120)
  private String fullName;

  @Column(name = "phone_number",nullable = false,length = 16)
  private String phoneNumber;
}
