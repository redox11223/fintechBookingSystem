package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.verification.InvalidEmailVerificationTokenException;
import com.redox.fintechBookingSystem.shared.utils.ProblemDetailsGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
class IdentityExceptionHandler{
  @ExceptionHandler(InvalidEmailVerificationTokenException.class)
  ResponseEntity<ProblemDetail> handleInvalidEmailVerificationToken(InvalidEmailVerificationTokenException ex,
                                                                           HttpServletRequest request){
    ProblemDetail response=ProblemDetailsGenerator.generate(HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            URI.create("https://api.citafin.dev/problems/invalid-email-verification-token"),
            "Invalid email verification token",
            URI.create(request.getRequestURI()),
            "EMAIL_VERIFICATION_TOKEN_INVALID"
            );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }
}
