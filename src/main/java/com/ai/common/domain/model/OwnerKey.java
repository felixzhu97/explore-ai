package com.ai.common.domain.model;

import java.util.Objects;
import lombok.Value;

/**
 * Data partition key: guest browser {@code c:{clientId}} or signed-in account {@code
 * u:{accountId}}.
 */
@Value
public class OwnerKey {
  String value;

  public static final String CLIENT_PREFIX = "c:";
  public static final String ACCOUNT_PREFIX = "u:";

  /** Rows without a visitor, such as session-less metrics; never matches a live visitor. */
  public static final OwnerKey UNOWNED = new OwnerKey("c:legacy-orphan");

  public OwnerKey(String value) {
    Objects.requireNonNull(value, "value");
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("owner key is required");
    }
    if (!trimmed.startsWith(CLIENT_PREFIX) && !trimmed.startsWith(ACCOUNT_PREFIX)) {
      throw new IllegalArgumentException("owner key must start with c: or u:");
    }
    if (trimmed.length() <= 2) {
      throw new IllegalArgumentException("owner key id is required");
    }
    value = trimmed;
    this.value = value;
  }

  /** Creates a guest owner key prefixed with {@code c:} for the given client id. */
  public static OwnerKey createClientKey(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("clientId is required");
    }
    return new OwnerKey(CLIENT_PREFIX + clientId.trim());
  }

  /** Creates a signed-in owner key prefixed with {@code u:} for the given account id. */
  public static OwnerKey createAccountKey(String accountId) {
    if (accountId == null || accountId.isBlank()) {
      throw new IllegalArgumentException("accountId is required");
    }
    return new OwnerKey(ACCOUNT_PREFIX + accountId.trim());
  }

  /** Wraps a stored owner key value. */
  public static OwnerKey parseKey(String text) {
    return new OwnerKey(text);
  }

  /** Tells whether the key belongs to a signed-in account. */
  public boolean isAccount() {
    return value.startsWith(ACCOUNT_PREFIX);
  }

  /** Tells whether the key belongs to a guest client. */
  public boolean isClient() {
    return value.startsWith(CLIENT_PREFIX);
  }

  /** Rejects any merge other than a live guest's rows moving into a signed-in account. */
  public void requireMergeableInto(OwnerKey target) {
    Objects.requireNonNull(target, "target");
    if (!isClient() || equals(UNOWNED) || !target.isAccount()) {
      throw new IllegalArgumentException("Only guest data can move into a signed-in account");
    }
  }
}
