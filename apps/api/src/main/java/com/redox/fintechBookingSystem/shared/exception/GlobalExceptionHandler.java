package com.redox.fintechBookingSystem.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex,
                                                              HttpServletRequest request) {
    ProblemDetail response = generateProblemDetail(HttpStatus.NOT_FOUND,
            ex.getMessage(),
            URI.create("https://api.citafin.dev/problems/resource-not-found"),
            "Resource not found",
            URI.create(request.getRequestURI()),
            "RESOURCE_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateResource(DuplicateResourceException ex,
                                                               HttpServletRequest request) {
    ProblemDetail response = generateProblemDetail(HttpStatus.CONFLICT,
            ex.getMessage(),
            URI.create("https://api.citafin.dev/problems/duplicate-resource"),
            "Duplicate resource",
            URI.create(request.getRequestURI()),
            "DUPLICATE_RESOURCE");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                HttpHeaders headers,
                                                                HttpStatusCode status,
                                                                WebRequest request) {

    // A configured MessageSource can override the title and detail when Spring builds the response.
    ProblemDetail problemDetail = ex.getBody();
    problemDetail.setType(URI.create("https://api.citafin.dev/problems/validation-error"));
    problemDetail.setTitle("Validation error");
    problemDetail.setDetail("Request contained invalid fields");
    problemDetail.setProperty("code", "VALIDATION_ERROR");
    var errors = ex.getBindingResult().getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                    FieldError::getField,
                    fieldError -> new ArrayList<>(List.of(Objects.requireNonNullElse(
                            fieldError.getDefaultMessage(), "Invalid field value"))),
                    (list1, list2) -> {
                      list1.addAll(list2);
                      return list1;
                    },
                    LinkedHashMap::new
            ));
    problemDetail.setProperty("errors", errors);
    return super.handleMethodArgumentNotValid(ex, headers, status, request);
  }

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
  private ProblemDetail generateProblemDetail(HttpStatus status,
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
