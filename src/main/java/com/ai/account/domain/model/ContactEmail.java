package com.ai.account.domain.model;

import java.util.regex.Pattern;
import lombok.Value;

/** Email address of an account; login handles and display names are never stored here. */
@Value
public class ContactEmail {
  String value;

  static final int MAX_LENGTH = 320;

  private static final Pattern SHAPE = Pattern.compile("^[^\\s@]+@[^\\s@]+$");

  public ContactEmail(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("email is required");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH || !SHAPE.matcher(value).matches()) {
      throw new IllegalArgumentException("email is not an email address");
    }
    this.value = value;
  }

  /** Returns the email, or {@code null} when the value is blank or not an email address. */
  public static ContactEmail parseOptionalEmail(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH || !SHAPE.matcher(trimmed).matches()) {
      return null;
    }
    return new ContactEmail(trimmed);
  }

  @Override
  public String toString() {
    return "ContactEmail[redacted]";
  }
}
