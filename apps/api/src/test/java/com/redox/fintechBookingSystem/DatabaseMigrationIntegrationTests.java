package com.redox.fintechBookingSystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationIntegrationTests {

  @Autowired
  private JdbcTemplate jdbc;

  @Test
  void rejectsCaseInsensitiveDuplicateEmail() {
    String email = "client-%s@example.com".formatted(UUID.randomUUID());
    insertUser(UUID.randomUUID(), email);

    assertThatThrownBy(() -> insertUser(UUID.randomUUID(), email.toUpperCase()))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_users_email_lower");
  }

  @Test
  void rejectsInvalidServiceDuration() {
    UUID categoryId = UUID.randomUUID();
    jdbc.update("""
            INSERT INTO service_categories (id, name)
            VALUES (?, ?)
            """, categoryId, "Category-%s".formatted(categoryId));

    assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO financial_services (id, category_id, name, duration_minutes)
            VALUES (?, ?, ?, ?)
            """, UUID.randomUUID(), categoryId, "Invalid duration", 17))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("chk_duration_valid");
  }

  @Test
  void rejectsInvertedTimeOffInterval() {
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-09-02T12:00:00-05:00");

    assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO time_offs (id, starts_at, ends_at, reason)
            VALUES (?, ?, ?, ?)
            """, UUID.randomUUID(), startsAt, startsAt.minusHours(1), "Invalid interval"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("chk_valid_offtime");
  }

  @Test
  void enforcesAppointmentOccupancyRules() {
    BookingFixture fixture = createBookingFixture();
    OffsetDateTime firstStart = OffsetDateTime.parse("2026-09-01T10:00:00-05:00");
    insertAppointment(fixture, fixture.firstAdvisorId(), firstStart,
            firstStart.plusHours(1), firstStart.plusMinutes(75), "CONFIRMED");

    assertThatCode(() -> insertAppointment(fixture, fixture.firstAdvisorId(),
            firstStart.plusMinutes(75), firstStart.plusMinutes(135),
            firstStart.plusMinutes(150), "CONFIRMED"))
            .doesNotThrowAnyException();

    assertThatCode(() -> insertAppointment(fixture, fixture.secondAdvisorId(),
            firstStart.plusMinutes(30), firstStart.plusMinutes(90),
            firstStart.plusMinutes(105), "CONFIRMED"))
            .doesNotThrowAnyException();

    assertThatCode(() -> insertAppointment(fixture, fixture.firstAdvisorId(),
            firstStart.plusMinutes(30), firstStart.plusMinutes(90),
            firstStart.plusMinutes(105), "CANCELLED"))
            .doesNotThrowAnyException();

    assertThatThrownBy(() -> insertAppointment(fixture, fixture.firstAdvisorId(),
            firstStart.plusMinutes(60), firstStart.plusMinutes(90),
            firstStart.plusMinutes(105), "CONFIRMED"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("exclude_advisor_double_booking");

    Integer acceptedAppointments = jdbc.queryForObject("""
            SELECT count(*) FROM appointments WHERE client_id = ?
            """, Integer.class, fixture.clientId());
    assertThat(acceptedAppointments).isEqualTo(4);
  }

  private BookingFixture createBookingFixture() {
    UUID clientUserId = UUID.randomUUID();
    UUID firstAdvisorUserId = UUID.randomUUID();
    UUID secondAdvisorUserId = UUID.randomUUID();
    insertUser(clientUserId, "client-%s@example.com".formatted(clientUserId));
    insertUser(firstAdvisorUserId, "advisor-%s@example.com".formatted(firstAdvisorUserId));
    insertUser(secondAdvisorUserId, "advisor-%s@example.com".formatted(secondAdvisorUserId));

    UUID clientId = UUID.randomUUID();
    UUID firstAdvisorId = UUID.randomUUID();
    UUID secondAdvisorId = UUID.randomUUID();
    jdbc.update("""
            INSERT INTO clients (id, user_id, full_name, phone_number)
            VALUES (?, ?, ?, ?)
            """, clientId, clientUserId, "Test Client", "+51999999999");
    insertAdvisor(firstAdvisorId, firstAdvisorUserId);
    insertAdvisor(secondAdvisorId, secondAdvisorUserId);

    UUID categoryId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    jdbc.update("INSERT INTO service_categories (id, name) VALUES (?, ?)",
            categoryId, "Category-%s".formatted(categoryId));
    jdbc.update("""
            INSERT INTO financial_services (id, category_id, name, duration_minutes)
            VALUES (?, ?, ?, ?)
            """, serviceId, categoryId, "Consultation", 60);

    return new BookingFixture(clientId, firstAdvisorId, secondAdvisorId, serviceId);
  }

  private void insertUser(UUID userId, String email) {
    jdbc.update("""
            INSERT INTO users (id, email, password_hash)
            VALUES (?, ?, ?)
            """, userId, email, "test-password-hash");
  }

  private void insertAdvisor(UUID advisorId, UUID userId) {
    jdbc.update("""
            INSERT INTO advisors (id, user_id, full_name, internal_code)
            VALUES (?, ?, ?, ?)
            """, advisorId, userId, "Test Advisor",
            "ADV-%s".formatted(advisorId.toString().substring(0, 20)));
  }

  private void insertAppointment(
          BookingFixture fixture,
          UUID advisorId,
          OffsetDateTime startsAt,
          OffsetDateTime endsAt,
          OffsetDateTime blockedUntil,
          String status
  ) {
    jdbc.update("""
            INSERT INTO appointments (
                id, client_id, advisor_id, service_id, starts_at, ends_at,
                blocked_until, status, reason, consented_at, consent_version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), fixture.clientId(), advisorId, fixture.serviceId(), startsAt,
            endsAt, blockedUntil, status, "Integration test", OffsetDateTime.now(), "v1");
  }

  private record BookingFixture(
          UUID clientId,
          UUID firstAdvisorId,
          UUID secondAdvisorId,
          UUID serviceId
  ) {
  }
}
