package com.redox.fintechBookingSystem.identity.authentication.exception;

public class MfaRequiredException extends RuntimeException{
  public MfaRequiredException(){
    super("Mfa verification is required");
  }
}
