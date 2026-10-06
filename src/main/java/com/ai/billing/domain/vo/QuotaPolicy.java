package com.ai.billing.domain.vo;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Daily limits for a plan: per owner, per client IP and across all clients.
 *
 * @param enforced whether the quota is checked at all
 * @param dailyLimit per-owner limit of the plan
 * @param ipDailyRequests configured per-IP ceiling, raised to the plan limit when lower
 * @param globalDailyRequests ceiling across all clients; 0 turns it off
 */
public record QuotaPolicy(
    boolean enforced, Plan plan, int dailyLimit, int ipDailyRequests, int globalDailyRequests) {

  public QuotaPolicy {
    Objects.requireNonNull(plan, "plan");
    if (dailyLimit < 0 || ipDailyRequests < 0 || globalDailyRequests < 0) {
      throw new IllegalArgumentException("quota limits must not be negative");
    }
  }

  /** Returns the per-IP limit, never below the plan limit so one client is never cut short. */
  public int ipDailyLimit() {
    return Math.max(ipDailyRequests, dailyLimit);
  }

  /** Returns the limit across all clients, or empty when it is turned off. */
  public OptionalInt globalDailyLimit() {
    return globalDailyRequests > 0 ? OptionalInt.of(globalDailyRequests) : OptionalInt.empty();
  }
}
