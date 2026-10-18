package com.ai.scim.controller;

import com.ai.common.controller.GlobalExceptionHandler;
import com.ai.common.exception.DomainException;
import com.ai.scim.infra.ScimErrors;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates failures on SCIM endpoints into RFC 7644 §3.12 error bodies. */
@Slf4j
@RestControllerAdvice(assignableTypes = {ScimUserController.class, ScimDiscoveryController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ScimExceptionHandler {

  /** Returns the SDK's own SCIM error. */
  @ExceptionHandler(ScimException.class)
  public ResponseEntity<ErrorResponse> handleScim(ScimException ex) {
    return ScimErrors.toResponse(ex.getScimError());
  }

  /** Maps domain rule failures to their HTTP status with a SCIM body. */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
    int status = GlobalExceptionHandler.statusOf(ex.getKind()).value();
    return ScimErrors.toResponse(ScimErrors.fromStatus(status, ex.getCode()));
  }

  /** Returns 400 invalidValue for values a value object rejects. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleInvalid(IllegalArgumentException ex) {
    return ScimErrors.toResponse(ScimErrors.error(400, "invalidValue", ex.getMessage()));
  }

  /** Returns 400 invalidSyntax for unreadable bodies. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return ScimErrors.toResponse(
        ScimErrors.error(400, "invalidSyntax", "request body is not valid SCIM JSON"));
  }

  /** Returns 500 without leaking details. */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(RuntimeException ex) {
    log.error("SCIM request failed: {}", ex.getClass().getName());
    return ScimErrors.toResponse(ScimErrors.error(500, null, "internal error"));
  }
}
