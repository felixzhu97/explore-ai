package com.ai.common.domain.exception;

/** Base type for domain-level failures surfaced to application services. */
public abstract class AbstractDomainException extends RuntimeException {

  protected AbstractDomainException(String message) {
    super(message);
  }

  protected AbstractDomainException(String message, Throwable cause) {
    super(message, cause);
  }
}
