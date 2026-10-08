package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  @PostMapping("/login")
  public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest loginRequest){

  }
}
