package com.redox.fintechBookingSystem.client;

import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ClientRegistrationServiceTests {
  private static final String EMAIL = "client@example.com";
  private static final String PASSWORD = "a sufficiently long password";
  private static final String EMAIL_CONSTRAINT = "uq_users_email_lower";

  @Mock private ClientRegistrationTransaction registrationTransaction;

  @Test
  void delegatesRegistrationWithNormalizedFullName() {
    ClientRegistrationService registrationService =
        new ClientRegistrationService(registrationTransaction);
    ClientRegistrationRequest request = new ClientRegistrationRequest(
        "  María\tQuispe   López  ", EMAIL, PASSWORD, "  +51987654321  ");

    registrationService.registerClient(request);

    verify(registrationTransaction).persistClient(same(request), eq("María Quispe López"));
  }

  @Test
  void absorbsOnlyTheExpectedDuplicateEmailViolation() {
    ClientRegistrationService registrationService =
        new ClientRegistrationService(registrationTransaction);
    DataIntegrityViolationException duplicateEmail = integrityViolation(
        "23505", EMAIL_CONSTRAINT, true);
    doThrow(duplicateEmail).when(registrationTransaction)
        .persistClient(any(), anyString());

    registrationService.registerClient(validRequest());
  }

  @Test
  void rejectsNullRequestBeforeUsingCollaborators() {
    ClientRegistrationService registrationService =
        new ClientRegistrationService(registrationTransaction);

    assertThatNullPointerException()
        .isThrownBy(() -> registrationService.registerClient(null));

    verifyNoInteractions(registrationTransaction);
  }

  @Test
  void propagatesViolationFromAnotherConstraint() {
    ClientRegistrationService registrationService =
        new ClientRegistrationService(registrationTransaction);
    DataIntegrityViolationException otherConstraint = integrityViolation(
        "23505", "uq_clients_user_id", false);
    doThrow(otherConstraint).when(registrationTransaction)
        .persistClient(any(), anyString());

    assertThatThrownBy(() -> registrationService.registerClient(validRequest()))
        .isSameAs(otherConstraint);
  }

  @Test
  void propagatesNonUniqueViolationForTheEmailConstraint() {
    ClientRegistrationService registrationService =
        new ClientRegistrationService(registrationTransaction);
    DataIntegrityViolationException nonUniqueViolation = integrityViolation(
        "23503", EMAIL_CONSTRAINT, false);
    doThrow(nonUniqueViolation).when(registrationTransaction)
        .persistClient(any(), anyString());

    assertThatThrownBy(() -> registrationService.registerClient(validRequest()))
        .isSameAs(nonUniqueViolation);
  }

  private static DataIntegrityViolationException integrityViolation(
      String sqlState, String constraintName, boolean nestedCause) {
    SQLException sqlException = new SQLException("database constraint violation", sqlState);
    ConstraintViolationException hibernateException =
        new ConstraintViolationException("constraint violation", sqlException, constraintName);
    Throwable cause = nestedCause ? new RuntimeException(hibernateException) : hibernateException;
    return new DataIntegrityViolationException("persistence failed", cause);
  }

  private static ClientRegistrationRequest validRequest() {
    return new ClientRegistrationRequest(
        "María Quispe", EMAIL, PASSWORD, "+51987654321");
  }
}
