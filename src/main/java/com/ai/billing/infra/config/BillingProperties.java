package com.ai.billing.infra.config;

import com.ai.billing.domain.vo.Plan;
import com.ai.billing.domain.vo.QuotaPolicy;
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

  /** Builds the quota policy for the configured plan; unknown plans fall back to free. */
  public QuotaPolicy toPolicy() {
    Plan active = Plan.parse(plan);
    int dailyLimit = active == Plan.PRO ? proDailyRequests : freeDailyRequests;
    return new QuotaPolicy(quotaEnabled, active, dailyLimit, ipDailyRequests, globalDailyRequests);
  }
}
