package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.authentication.AuthenticatedLogin;
import com.redox.fintechBookingSystem.identity.authentication.LoginService;
import com.redox.fintechBookingSystem.identity.dto.LoginRequest;
import com.redox.fintechBookingSystem.identity.dto.LoginResponse;
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
public class AuthController {
  private final LoginService loginService;
  private final RefreshCookieFactory refreshCookieFactory;

  @PostMapping("/login")
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
