package com.redox.fintechBookingSystem;

import com.redox.fintechBookingSystem.catalog.ServiceCategory;
import com.redox.fintechBookingSystem.catalog.ServiceCategoryRepo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaAuditingIntegrationTest {
  @Autowired
  private ServiceCategoryRepo categoryRepo;

  @Autowired
  private EntityManager entityManager;

  @Test
  void createdAtAndUpdatedAtShouldWorkProperly() {
    ServiceCategory serviceCategory=new ServiceCategory();
    serviceCategory.setName("Test Category");

    ServiceCategory saved=categoryRepo.saveAndFlush(serviceCategory);

    //refresh the entity to use the values from the database not the ones in memory because
    //Instant can contain nanosecond precision but TIMESTAMPZ only has microsecond precision
    entityManager.refresh(saved);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());

    Instant originalCreatedAt=saved.getCreatedAt();
    Instant originalUpdatedAt=saved.getUpdatedAt();

    saved.setDescription("New Description");
    categoryRepo.saveAndFlush(saved);

    entityManager.clear();

    ServiceCategory reloaded=categoryRepo.findById(saved.getId()).orElseThrow(() ->
            new AssertionError("Entity should exist in the database"));
    assertThat(reloaded.getCreatedAt()).isEqualTo(originalCreatedAt);
    assertThat(reloaded.getUpdatedAt()).isAfter(originalUpdatedAt);
  }
}
