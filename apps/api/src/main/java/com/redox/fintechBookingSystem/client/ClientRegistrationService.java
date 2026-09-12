package com.redox.fintechBookingSystem.client;

import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import com.redox.fintechBookingSystem.identity.IdentityRegistrationService;
import com.redox.fintechBookingSystem.identity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientRegistrationService {
  private final ClientRepo clientRepo;
  private final IdentityRegistrationService identityService;

  @Transactional
  public void registerClient(ClientRegistrationRequest registrationRequest){
    Objects.requireNonNull(registrationRequest);
    String formattedFullName=formatClientName(registrationRequest.fullName());
    Optional<User> user=identityService.registerPendingUser(
            registrationRequest.email(),
            registrationRequest.password()
    );
    if(user.isPresent()){
      Client newClient=new Client(
              user.get(),
              formattedFullName,
              registrationRequest.phoneNumber().strip());
      clientRepo.save(newClient);
    }
  }
  private String formatClientName(String fullName){
    return fullName.strip().replaceAll("\\s+"," ");
  }
}
