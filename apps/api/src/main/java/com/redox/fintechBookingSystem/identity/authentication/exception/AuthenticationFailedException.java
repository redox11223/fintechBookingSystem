package com.redox.fintechBookingSystem.identity.authentication.exception;

public class AuthenticationFailedException extends RuntimeException{
  public AuthenticationFailedException(){
    super("Authentication Failed");
  }
}
