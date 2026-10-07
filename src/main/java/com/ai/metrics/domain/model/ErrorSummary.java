package com.ai.metrics.domain.model;

import java.util.regex.Pattern;

/**
 * Error code plus a normalized, length-capped message of a failed invocation.
 *
 * @param code exception type or error code, at most {@value #MAX_CODE_LENGTH} characters
 * @param message single-line message, at most {@value #MAX_MESSAGE_LENGTH} characters, or null
 */
public record ErrorSummary(String code, String message) {

  public static final int MAX_CODE_LENGTH = 64;
  public static final int MAX_MESSAGE_LENGTH = 512;

  static final String UNKNOWN_CODE = "unknown";

  private static final Pattern CONTROL_AND_SPACE = Pattern.compile("[\\p{Cntrl}\\s]+");

  public ErrorSummary {
    code = cap(clean(code), MAX_CODE_LENGTH);
    if (code == null) {
      code = UNKNOWN_CODE;
    }
    message = cap(clean(message), MAX_MESSAGE_LENGTH);
  }

  /** Summarizes an exception by its type and message. */
  public static ErrorSummary of(Throwable error) {
    if (error == null) {
      return new ErrorSummary(UNKNOWN_CODE, null);
    }
    String type = error.getClass().getSimpleName();
    return new ErrorSummary(type.isEmpty() ? error.getClass().getName() : type, error.getMessage());
  }

  /** Summarizes an error from a code and a message. */
  public static ErrorSummary of(String code, String message) {
    return new ErrorSummary(code, message);
  }

  private static String clean(String value) {
    if (value == null) {
      return null;
    }
    String collapsed = CONTROL_AND_SPACE.matcher(value).replaceAll(" ").trim();
    return collapsed.isEmpty() ? null : collapsed;
  }

  private static String cap(String value, int max) {
    if (value == null || value.length() <= max) {
      return value;
    }
    int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
    return value.substring(0, end);
  }
}
