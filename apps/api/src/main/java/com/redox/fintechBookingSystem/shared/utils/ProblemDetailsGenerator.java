package com.redox.fintechBookingSystem.shared.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public final class ProblemDetailsGenerator {
  private ProblemDetailsGenerator(){}

  /**
   * Generates a ProblemDetail instance with the specified parameters.
   *
   * @param status   The HTTP status.
   * @param detail   The detail message.
   * @param type     The type of the problem.
   * @param title    The title of the problem.
   * @param instance The instance of the problem.
   * @param code     The code of the problem.
   * @return A ProblemDetail instance.
   */
  public static ProblemDetail generate(HttpStatus status,
                                                    String detail,
                                                    URI type,
                                                    String title,
                                                    URI instance,
                                                    String code) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setType(type);
    problemDetail.setTitle(title);
    problemDetail.setInstance(instance);
    problemDetail.setProperty("code", code);
    return problemDetail;
  }
}
