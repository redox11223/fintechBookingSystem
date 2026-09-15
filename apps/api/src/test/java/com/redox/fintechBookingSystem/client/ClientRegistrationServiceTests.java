package com.redox.fintechBookingSystem.client;

import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import com.redox.fintechBookingSystem.client.repo.ClientRepo;
import com.redox.fintechBookingSystem.identity.IdentityRegistrationService;
import com.redox.fintechBookingSystem.identity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientRegistrationServiceTests {
  private static final String EMAIL = "client@example.com";
  private static final String PASSWORD = "a sufficiently long password";

  @Mock private ClientRepo clientRepo;
  @Mock private IdentityRegistrationService identityRegistrationService;

  @Test
  void createsClientWithNormalizedProfileForNewIdentity() {
    User user = new User();
    when(identityRegistrationService.registerPendingUser(EMAIL, PASSWORD))
        .thenReturn(Optional.of(user));
    ClientRegistrationService registrationService =
        new ClientRegistrationService(clientRepo, identityRegistrationService);
    ClientRegistrationRequest request = new ClientRegistrationRequest(
        "  María\tQuispe   López  ", EMAIL, PASSWORD, "  +51987654321  ");

    registrationService.registerClient(request);

    ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
    verify(clientRepo).save(clientCaptor.capture());
    Client client = clientCaptor.getValue();
    assertThat(client.getUser()).isSameAs(user);
    assertThat(client.getFullName()).isEqualTo("María Quispe López");
    assertThat(client.getPhoneNumber()).isEqualTo("+51987654321");
  }

  @Test
  void existingIdentityDoesNotCreateAnotherClient() {
    when(identityRegistrationService.registerPendingUser(EMAIL, PASSWORD))
        .thenReturn(Optional.empty());
    ClientRegistrationService registrationService =
        new ClientRegistrationService(clientRepo, identityRegistrationService);

    registrationService.registerClient(validRequest());

    verify(clientRepo, never()).save(any(Client.class));
  }

  @Test
  void rejectsNullRequestBeforeUsingCollaborators() {
    ClientRegistrationService registrationService =
        new ClientRegistrationService(clientRepo, identityRegistrationService);

    assertThatNullPointerException()
        .isThrownBy(() -> registrationService.registerClient(null));

    verifyNoInteractions(identityRegistrationService, clientRepo);
  }

  @Test
  void identityFailureDoesNotAttemptToPersistClient() {
    RuntimeException failure = new RuntimeException("identity persistence failed");
    when(identityRegistrationService.registerPendingUser(EMAIL, PASSWORD)).thenThrow(failure);
    ClientRegistrationService registrationService =
        new ClientRegistrationService(clientRepo, identityRegistrationService);

    assertThatThrownBy(() -> registrationService.registerClient(validRequest()))
        .isSameAs(failure);

    verifyNoInteractions(clientRepo);
  }

  private static ClientRegistrationRequest validRequest() {
    return new ClientRegistrationRequest(
        "María Quispe", EMAIL, PASSWORD, "+51987654321");
  }
}
