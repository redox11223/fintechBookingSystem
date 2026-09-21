package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.dto.ConfirmVerificationEmailRequest;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {
  private final EmailVerificationConfirmationService verificationRequestService;

  @PostMapping("/confirm")
  public ResponseEntity<Void> verifyEmail(@Valid @RequestBody ConfirmVerificationEmailRequest emailRequest){
    verificationRequestService.confirm(emailRequest.token());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
