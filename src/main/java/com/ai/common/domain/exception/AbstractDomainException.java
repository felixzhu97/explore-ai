package com.ai.common.domain.exception;

/** Base type for domain-level failures surfaced to application services. */
public abstract class AbstractDomainException extends RuntimeException {

  /** Documentation. */
  protected AbstractDomainException(String message) {
    super(message);
  }

  /** Documentation. */
  protected AbstractDomainException(String message, Throwable cause) {
    super(message, cause);
  }
}
