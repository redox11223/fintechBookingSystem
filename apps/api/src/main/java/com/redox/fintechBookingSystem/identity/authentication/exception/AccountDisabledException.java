package com.redox.fintechBookingSystem.identity.authentication.exception;

public class AccountDisabledException extends RuntimeException{
  public AccountDisabledException(){
    super("Account is disabled");
  }
}
