package com.ai.account.domain.model;

import java.util.regex.Pattern;

/** UUID carried by the Client Identity cookie; only server-issued ids are accepted. */
public record ClientId(String value) {

  private static final Pattern UUID_SHAPE =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public ClientId {
    if (value == null || !UUID_SHAPE.matcher(value.trim()).matches()) {
      throw new IllegalArgumentException("client id must be a UUID");
    }
    value = value.trim();
  }

  /** Parses a Client Identity value, rejecting anything that is not a UUID. */
  public static ClientId parseId(String raw) {
    return new ClientId(raw);
  }

  /** Tells whether the raw value is a valid client id. */
  public static boolean isValid(String raw) {
    return raw != null && UUID_SHAPE.matcher(raw.trim()).matches();
  }
}
