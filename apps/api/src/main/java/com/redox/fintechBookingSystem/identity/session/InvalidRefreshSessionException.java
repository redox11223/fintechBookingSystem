package com.redox.fintechBookingSystem.identity.session;

public class InvalidRefreshSessionException extends RuntimeException{
  public InvalidRefreshSessionException(){
    super("Invalid session");
  }
}
