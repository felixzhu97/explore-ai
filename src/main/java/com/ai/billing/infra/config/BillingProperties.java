package com.ai.billing.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Billing settings: quota toggle, active plan, and daily request limits per plan. */
@ConfigurationProperties(prefix = "app.billing")
@Getter
@Setter
public class BillingProperties {

  private boolean quotaEnabled = true;

  /** Billing plan: free or pro. */
  private String plan = "free";

  private int freeDailyRequests = 50;
  private int proDailyRequests = 2000;

  /** Daily ceiling per client IP, so dropping the identity cookie does not reset the quota. */
  private int ipDailyRequests = 200;

  /** Daily ceiling across all clients; 0 disables it. */
  private int globalDailyRequests = 5000;

  /** Returns the daily request limit for the active plan, using the free limit unless pro. */
  public int resolveDailyLimit() {
    if ("pro".equalsIgnoreCase(plan)) {
      return proDailyRequests;
    }
    return freeDailyRequests;
  }

  /** Returns the per-IP daily limit, never below the per-client plan limit. */
  public int resolveIpDailyLimit() {
    return Math.max(ipDailyRequests, resolveDailyLimit());
  }
}
