package com.redox.fintechBookingSystem.identity.verification;

public class InvalidEmailVerificationTokenException extends RuntimeException{
  public InvalidEmailVerificationTokenException(){
    super("Email verification token is invalid");
  }
}
