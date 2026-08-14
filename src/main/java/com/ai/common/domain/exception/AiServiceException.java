package com.ai.common.domain.exception;

/** Exception thrown when AI service encounters an error. */
public class AiServiceException extends RuntimeException {

  private final String errorCode;

  /** Documentation. */
  public AiServiceException(String message) {
    super(message);
    this.errorCode = "AI_SERVICE_ERROR";
  }

  /** Documentation. */
  public AiServiceException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "AI_SERVICE_ERROR";
  }

  /** Documentation. */
  public AiServiceException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
