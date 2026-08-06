package com.redox.fintechBookingSystem.client;

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
@Table(name = "clients",uniqueConstraints = {
        @UniqueConstraint(name = "uq_client_document",columnNames = {"document_type","document_number"})
})
public class Client extends BaseEntity {
  //in oneToOne and ManyToOne relationships the default is eager change it to lazy,joinColumn should
  //be in the entity that has the foreign key
  @OneToOne(fetch = FetchType.LAZY,optional = false)
  @JoinColumn(name = "user_id",nullable = false,unique = true)
  private User user;

  @Column(name = "full_name",nullable = false,length = 100)
  private String fullName;

  @Column(name = "document_type",nullable = false,length = 20)
  private String documentType;

  @Column(name = "document_number",nullable = false,length = 20)
  private String documentNumber;

  @Column(name = "phone_number",nullable = false,length = 20)
  private String phoneNumber;
}
