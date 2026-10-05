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

  /** Returns the daily request limit for the active plan, using the free limit unless pro. */
  public int dailyLimit() {
    if ("pro".equalsIgnoreCase(plan)) {
      return proDailyRequests;
    }
    return freeDailyRequests;
  }
}
