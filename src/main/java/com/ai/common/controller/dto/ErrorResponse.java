package com.ai.common.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/** Documentation. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, String errorCode, Instant timestamp, String path) {
  /** Documentation. */
  public static ErrorResponse of(String message, String errorCode) {
    return new ErrorResponse(message, errorCode, Instant.now(), null);
  }

  /** Documentation. */
  public static ErrorResponse of(String message, String errorCode, String path) {
    return new ErrorResponse(message, errorCode, Instant.now(), path);
  }
}
