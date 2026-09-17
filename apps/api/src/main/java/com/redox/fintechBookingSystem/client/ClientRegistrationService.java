package com.redox.fintechBookingSystem.client;

import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Public registration facade. It normalizes client input and hides only the expected
 * duplicate-email race while allowing unrelated persistence failures to propagate.
 */
@Service
@RequiredArgsConstructor
public class ClientRegistrationService {
  private final ClientRegistrationTransaction registrationTransaction;
  private final static String UNIQUE_VIOLATION_CODE="23505";
  private final static String CONSTRAINT_NAME="uq_users_email_lower";

  public void registerClient(ClientRegistrationRequest registrationRequest){
    Objects.requireNonNull(registrationRequest);
    String formattedFullName=formatClientName(registrationRequest.fullName());
    try {
      registrationTransaction.persistClient(registrationRequest,formattedFullName);
    } catch (DataIntegrityViolationException ex) {
      var constraintViolationEx=findCause(ex,ConstraintViolationException.class);
      if(constraintViolationEx!=null){
        String constraintName=constraintViolationEx.getConstraintName();
        String sqlState=constraintViolationEx.getSQLState();
        if(CONSTRAINT_NAME.equals(constraintName) && UNIQUE_VIOLATION_CODE.equals(sqlState)){
          return;
        }
      }
      throw ex;
    }
  }
  private String formatClientName(String fullName){
    return fullName.strip().replaceAll("\\s+"," ");
  }

  private static <T extends Throwable> T findCause(Throwable throwable,Class<T> targetType){
    Throwable current=throwable;
    while(current!=null){
      if(targetType.isInstance(current)){
        return targetType.cast(current);
      }
      current=current.getCause();
    }
    return null;
  }
}
