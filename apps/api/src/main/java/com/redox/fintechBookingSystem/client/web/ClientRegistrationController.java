package com.redox.fintechBookingSystem.client.web;

import com.redox.fintechBookingSystem.client.ClientRegistrationService;
import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import com.redox.fintechBookingSystem.client.dto.RegistrationAcceptedResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class ClientRegistrationController {
  private final ClientRegistrationService registrationService;

  @PostMapping("/register")
  @ApiResponses({
      @ApiResponse(
          responseCode = "202",
          description = "Registration was processed",
          content = @Content(schema = @Schema(implementation = RegistrationAcceptedResponse.class))),
      @ApiResponse(
          responseCode = "400",
          description = "Request validation failed",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<RegistrationAcceptedResponse> registerClient(
      @Valid @RequestBody ClientRegistrationRequest registrationRequest) {
    registrationService.registerClient(registrationRequest);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new RegistrationAcceptedResponse("Registration processed"));
  }
}
