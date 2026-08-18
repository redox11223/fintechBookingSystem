package com.redox.fintechBookingSystem.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT,reason = "Duplicate resource")
public class DuplicateResourceException extends RuntimeException{
  public DuplicateResourceException(String message){
    super(message);
  }
}
