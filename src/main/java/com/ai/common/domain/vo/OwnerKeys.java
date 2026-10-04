package com.ai.common.domain.vo;

/** Owner-key helpers for client and account partitions. */
public final class OwnerKeys {

  private OwnerKeys() {}

  public static OwnerKey requireClient(String clientId) {
    return OwnerKey.forClient(clientId);
  }

  /** Extracts the client id from a guest owner key, rejecting null or account keys. */
  public static String parseClientId(OwnerKey ownerKey) {
    if (ownerKey == null) {
      throw new IllegalArgumentException("ownerKey is required");
    }
    if (!ownerKey.isClient()) {
      throw new IllegalArgumentException("owner key is not a client partition");
    }
    return ownerKey.value().substring(OwnerKey.CLIENT_PREFIX.length());
  }

  public static String toStorageValue(OwnerKey ownerKey) {
    return ownerKey == null ? null : ownerKey.value();
  }
}
