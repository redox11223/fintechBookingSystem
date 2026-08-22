package com.redox.fintechBookingSystem.shared.exception;


public class ResourceNotFoundException extends RuntimeException{
  public ResourceNotFoundException(String message){
    super(message);
  }
}
