package com.ai.common.domain.vo;

import java.util.Objects;

/**
 * Data partition key: guest browser {@code c:{clientId}} or signed-in account {@code
 * u:{accountUserId}}.
 */
public record OwnerKey(String value) {

  public static final String CLIENT_PREFIX = "c:";
  public static final String ACCOUNT_PREFIX = "u:";

  /** Rows without a visitor, such as session-less metrics; never matches a live visitor. */
  public static final OwnerKey UNOWNED = new OwnerKey("c:legacy-orphan");

  public OwnerKey {
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
  }

  /** Creates a guest owner key prefixed with {@code c:} for the given client id. */
  public static OwnerKey forClient(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("clientId is required");
    }
    return new OwnerKey(CLIENT_PREFIX + clientId.trim());
  }

  /** Creates a signed-in owner key prefixed with {@code u:} for the given account user id. */
  public static OwnerKey forAccount(String accountUserId) {
    if (accountUserId == null || accountUserId.isBlank()) {
      throw new IllegalArgumentException("accountUserId is required");
    }
    return new OwnerKey(ACCOUNT_PREFIX + accountUserId.trim());
  }

  /** Wraps a stored owner key value. */
  public static OwnerKey parse(String raw) {
    return new OwnerKey(raw);
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
