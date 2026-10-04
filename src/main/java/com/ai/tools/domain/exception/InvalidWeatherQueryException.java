package com.ai.tools.domain.exception;

/** Thrown when a weather query has an invalid or missing city. */
public class InvalidWeatherQueryException extends RuntimeException {
  public InvalidWeatherQueryException(String message) {
    super(message);
  }
}
