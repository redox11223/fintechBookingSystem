package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.dto.ConfirmVerificationEmailRequest;
import com.redox.fintechBookingSystem.identity.verification.EmailVerificationConfirmationService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication")
public class EmailVerificationController {
  private final EmailVerificationConfirmationService verificationConfirmationService;

  @PostMapping("/confirm")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Email address was verified"),
      @ApiResponse(
          responseCode = "400",
          description = "Request or verification token is invalid",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Void> verifyEmail(@Valid @RequestBody ConfirmVerificationEmailRequest emailRequest){
    verificationConfirmationService.confirm(emailRequest.token());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
