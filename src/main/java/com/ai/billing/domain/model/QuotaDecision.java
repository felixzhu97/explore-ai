package com.ai.billing.domain.model;

import java.util.Objects;
import lombok.Value;

/** Outcome of one quota check; the single source of the {@code X-Quota-*} response headers. */
@Value
public class QuotaDecision {
  boolean allowed;
  Plan plan;
  int limit;
  int remaining;

  public QuotaDecision(boolean allowed, Plan plan, int limit, int remaining) {
    Objects.requireNonNull(plan, "plan");
    if (limit < 0 || remaining < 0 || remaining > limit) {
      throw new IllegalArgumentException("remaining must be between 0 and limit");
    }
    if (!allowed && remaining != 0) {
      throw new IllegalArgumentException("a refused request has nothing remaining");
    }
    this.allowed = allowed;
    this.plan = plan;
    this.limit = limit;
    this.remaining = remaining;
  }

  /** Allows the request with the requests left under the limit. */
  public static QuotaDecision createApproval(Plan plan, int limit, int remaining) {
    return new QuotaDecision(true, plan, limit, remaining);
  }

  /** Refuses the request; nothing is remaining, whichever limit ran out. */
  public static QuotaDecision createRefusal(Plan plan, int limit) {
    return new QuotaDecision(false, plan, limit, 0);
  }
}
