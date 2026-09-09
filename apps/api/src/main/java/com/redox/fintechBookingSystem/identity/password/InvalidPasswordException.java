package com.redox.fintechBookingSystem.identity.password;

public class InvalidPasswordException extends RuntimeException{
  private final PasswordViolation violation;
  public InvalidPasswordException(PasswordViolation violation) {
    super("Invalid password does not meet the policy requirements");
    this.violation = violation;
  }

  public PasswordViolation getViolation() {
    return violation;
  }
}
