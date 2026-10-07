package com.ai.billing.domain.model;

import com.ai.common.domain.model.OwnerKey;
import java.util.Objects;

/** Who a quota counter belongs to; each kind has its own prefix so counters never collide. */
public record QuotaSubject(String key) {

  public static final QuotaSubject GLOBAL = new QuotaSubject("global");

  private static final String OWNER_PREFIX = "owner:";
  private static final String ADDRESS_PREFIX = "address:";

  public QuotaSubject {
    Objects.requireNonNull(key, "key");
  }

  /** Counter of a data owner, shared by HTTP requests and scheduled automations. */
  public static QuotaSubject owner(OwnerKey ownerKey) {
    return new QuotaSubject(OWNER_PREFIX + ownerKey.value());
  }

  /** Counter of a client IP address. */
  public static QuotaSubject address(String ip) {
    if (ip == null || ip.isBlank()) {
      throw new IllegalArgumentException("ip is required");
    }
    return new QuotaSubject(ADDRESS_PREFIX + ip.trim());
  }

  @Override
  public String toString() {
    return "QuotaSubject[" + key.substring(0, key.indexOf(':') + 1) + "redacted]";
  }
}
