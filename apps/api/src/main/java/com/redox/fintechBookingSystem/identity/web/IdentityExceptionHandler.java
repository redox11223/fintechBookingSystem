package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.authentication.exception.AccountDisabledException;
import com.redox.fintechBookingSystem.identity.authentication.exception.AuthenticationFailedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.EmailNotVerifiedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.MfaRequiredException;
import com.redox.fintechBookingSystem.identity.verification.InvalidEmailVerificationTokenException;
import com.redox.fintechBookingSystem.shared.utils.ProblemDetailsGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = EmailVerificationController.class)
class IdentityExceptionHandler{
  @ExceptionHandler(InvalidEmailVerificationTokenException.class)
  ResponseEntity<ProblemDetail> handleInvalidEmailVerificationToken(InvalidEmailVerificationTokenException ex,
                                                                           HttpServletRequest request){
    ProblemDetail response=ProblemDetailsGenerator.generate(HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            "invalid-email-verification-token",
            "Invalid email verification token",
            request,
            "EMAIL_VERIFICATION_TOKEN_INVALID"
            );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }
  @ExceptionHandler(AuthenticationFailedException.class)
  ResponseEntity<ProblemDetail> handleAuthenticationFailedException(AuthenticationFailedException ex,
                                                                    HttpServletRequest request){
    ProblemDetail response=ProblemDetailsGenerator.generate(HttpStatus.UNAUTHORIZED,
            ex.getMessage(),
            "Authentication-failed",
            "Authentication failed",
            request,
            "AUTHENTICATION_FAILED"
            );
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }
  @ExceptionHandler(AccountDisabledException.class)
  ResponseEntity<ProblemDetail> handleAccountDisabledException(AccountDisabledException ex,
                                                                    HttpServletRequest request){
    ProblemDetail response=ProblemDetailsGenerator.generate(HttpStatus.FORBIDDEN,
            ex.getMessage(),
            "Account-disabled",
            "Account disabled",
            request,
            "ACCOUNT_DISABLED"
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }
  @ExceptionHandler(EmailNotVerifiedException.class)
  ResponseEntity<ProblemDetail> handleEmailNotVerifiedException(EmailNotVerifiedException ex,
                                                                    HttpServletRequest request){
    ProblemDetail response=ProblemDetailsGenerator.generate(HttpStatus.FORBIDDEN,
            ex.getMessage(),
            "Email-not-verified",
            "Email not verified",
            request,
            "EMAIL_NOT_VERIFIED"
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }
  @ExceptionHandler(MfaRequiredException.class)
  ResponseEntity<ProblemDetail> handleMfaRequiredException(MfaRequiredException ex,
                                                                HttpServletRequest request){
    ProblemDetail response=ProblemDetailsGenerator.generate(HttpStatus.FORBIDDEN,
            ex.getMessage(),
            "Mfa-required",
            "Mfa required",
            request,
            "MFA_REQUIRED"
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }
}
