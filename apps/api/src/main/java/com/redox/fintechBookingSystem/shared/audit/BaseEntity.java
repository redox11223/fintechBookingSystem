package com.redox.fintechBookingSystem.shared.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class) //check jpa entity listeners and callbacks
@Getter @Setter
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at",nullable = false,updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;

    // Tolerancia a Proxies de Hibernate: obtiene la clase real detrás del Proxy
    Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();

    Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();

    if (thisEffectiveClass != oEffectiveClass) return false;

    BaseEntity entity = (BaseEntity) o;
    // Dos entidades sin ID (no guardadas aún) no son iguales
    return getId() != null && Objects.equals(getId(), entity.getId());
  }

  @Override
  public final int hashCode() {
    // Devuelve un valor constante para entidades del mismo tipo.
    // Esto garantiza que el hashCode nunca cambie cuando el ID pase de null a tener valor al hacer save().
    return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
            : getClass().hashCode();
  }
}
