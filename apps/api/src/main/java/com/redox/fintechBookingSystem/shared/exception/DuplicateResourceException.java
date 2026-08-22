package com.redox.fintechBookingSystem.shared.exception;


public class DuplicateResourceException extends RuntimeException{
  public DuplicateResourceException(String message){
    super(message);
  }
}
