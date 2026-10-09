package com.redox.fintechBookingSystem.identity.authentication.exception;

public class EmailNotVerifiedException extends RuntimeException{
  public EmailNotVerifiedException(){
    super("Email address is not verified");
  }
}
