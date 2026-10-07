package com.ai.common.exception;

/** A business rule failure with a stable error code that the API returns to clients. */
public class DomainException extends RuntimeException {

  /** What went wrong, independent of transport; the API maps each kind to one HTTP status. */
  public enum Kind {
    NOT_FOUND,
    CONFLICT,
    INVALID,
    LIMIT_EXCEEDED,
    UNPROCESSABLE,
    UNAVAILABLE,
    FAILED
  }

  private final Kind kind;
  private final String code;

  public DomainException(Kind kind, String code, String message) {
    this(kind, code, message, null);
  }

  public DomainException(Kind kind, String code, String message, Throwable cause) {
    super(message, cause);
    this.kind = kind;
    this.code = code;
  }

  /** Creates the error for a missing resource. */
  public static DomainException createNotFoundError(String code, String message) {
    return new DomainException(Kind.NOT_FOUND, code, message);
  }

  /** Creates the error for a name or key that is already taken. */
  public static DomainException createConflictError(String code, String message) {
    return new DomainException(Kind.CONFLICT, code, message);
  }

  /** Creates the error for input that breaks a rule. */
  public static DomainException createInvalidError(String code, String message) {
    return new DomainException(Kind.INVALID, code, message);
  }

  /** Creates the error for a quota that is used up. */
  public static DomainException createLimitExceededError(String code, String message) {
    return new DomainException(Kind.LIMIT_EXCEEDED, code, message);
  }

  /** Creates the error for well-formed input that cannot be processed. */
  public static DomainException createUnprocessableError(String code, String message) {
    return new DomainException(Kind.UNPROCESSABLE, code, message);
  }

  /** Creates the error for a provider that is turned off, unconfigured or down. */
  public static DomainException createUnavailableError(String code, String message) {
    return new DomainException(Kind.UNAVAILABLE, code, message);
  }

  /** Creates the error for a provider that is turned off, unconfigured or down. */
  public static DomainException createUnavailableError(
      String code, String message, Throwable cause) {
    return new DomainException(Kind.UNAVAILABLE, code, message, cause);
  }

  /** Creates the error for an operation that failed while running. */
  public static DomainException createFailedError(String code, String message) {
    return new DomainException(Kind.FAILED, code, message);
  }

  /** Creates the error for an operation that failed while running. */
  public static DomainException createFailedError(String code, String message, Throwable cause) {
    return new DomainException(Kind.FAILED, code, message, cause);
  }

  /** Returns what went wrong. */
  public Kind getKind() {
    return kind;
  }

  /** Returns the error code sent to clients. */
  public String getCode() {
    return code;
  }
}
