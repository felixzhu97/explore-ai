package com.ai.account.domain.model;

import java.util.regex.Pattern;
import lombok.Value;

/** UUID carried by the Client Identity cookie; only server-issued ids are accepted. */
@Value
public class ClientId {
  String value;

  private static final Pattern UUID_SHAPE =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public ClientId(String value) {
    if (value == null || !UUID_SHAPE.matcher(value.trim()).matches()) {
      throw new IllegalArgumentException("client id must be a UUID");
    }
    value = value.trim();
    this.value = value;
  }

  /** Parses a Client Identity value, rejecting anything that is not a UUID. */
  public static ClientId parseId(String text) {
    return new ClientId(text);
  }

  /** Tells whether the text value is a valid client id. */
  public static boolean isValid(String text) {
    return text != null && UUID_SHAPE.matcher(text.trim()).matches();
  }
}
