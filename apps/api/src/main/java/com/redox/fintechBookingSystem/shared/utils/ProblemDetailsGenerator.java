package com.redox.fintechBookingSystem.shared.utils;

import jakarta.servlet.http.HttpServletRequest;
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
   * @param slug     The specific slug of the problem.
   * @param title    The title of the problem.
   * @param request The request of the problem.
   * @param code     The code of the problem.
   * @return A ProblemDetail instance.
   */
  public static ProblemDetail generate(HttpStatus status,
                                                    String detail,
                                                    String slug,
                                                    String title,
                                                    HttpServletRequest request,
                                                    String code) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setType(URI.create("https://api.citafin.dev/problems/"+slug));
    problemDetail.setTitle(title);
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("code", code);
    return problemDetail;
  }
}
