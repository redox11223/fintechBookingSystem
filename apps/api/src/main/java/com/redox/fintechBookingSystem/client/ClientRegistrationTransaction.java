package com.redox.fintechBookingSystem.client;

import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import com.redox.fintechBookingSystem.client.repo.ClientRepo;
import com.redox.fintechBookingSystem.identity.IdentityRegistrationService;
import com.redox.fintechBookingSystem.identity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persists the identity and client profile atomically. The explicit flush surfaces database
 * constraint failures before returning so the outer facade can classify them after rollback.
 */
@Component
@RequiredArgsConstructor
public class ClientRegistrationTransaction {
  private final IdentityRegistrationService identityService;
  private final ClientRepo clientRepo;

  @Transactional
  public void persistClient(ClientRegistrationRequest registrationRequest,String formattedFullName){
    Optional<User> user=identityService.registerPendingUser(
            registrationRequest.email(),
            registrationRequest.password()
    );
    if(user.isPresent()){
      Client newClient=new Client(
              user.get(),
              formattedFullName,
              registrationRequest.phoneNumber().strip());
      clientRepo.saveAndFlush(newClient);
    }
  }

}
