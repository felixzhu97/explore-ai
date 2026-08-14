package com.ai.rag.domain.exception;

/** Documentation. */
public class RagServiceException extends RuntimeException {
  /** Documentation. */
  public RagServiceException(String message) {
    super(message);
  }

  /** Documentation. */
  public RagServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
