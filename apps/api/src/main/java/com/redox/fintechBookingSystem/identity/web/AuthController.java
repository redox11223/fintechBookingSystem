package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.authentication.AuthenticatedLogin;
import com.redox.fintechBookingSystem.identity.authentication.LoginService;
import com.redox.fintechBookingSystem.identity.dto.LoginRequest;
import com.redox.fintechBookingSystem.identity.dto.LoginResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
  private final LoginService loginService;
  private final RefreshCookieFactory refreshCookieFactory;

  @PostMapping("/login")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Authentication succeeded",
          headers = @Header(
              name = HttpHeaders.SET_COOKIE,
              description = "Host-only HttpOnly refresh-token cookie"),
          content = @Content(schema = @Schema(implementation = LoginResponse.class))),
      @ApiResponse(
          responseCode = "400",
          description = "Request validation failed",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(
          responseCode = "401",
          description = "Credentials are invalid or the account is temporarily locked",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(
          responseCode = "403",
          description = "Account state prevents completing this login",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
     AuthenticatedLogin authenticated=loginService.login(loginRequest.email(), loginRequest.password());
    ResponseCookie cookie= refreshCookieFactory.create(authenticated.refreshToken().rawToken());
     LoginResponse response=new LoginResponse(
             authenticated.accessToken().value(),
             "Bearer",
             authenticated.accessTokenExpiresInSeconds()
             );
     return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie.toString()).body(response);
  }
}
