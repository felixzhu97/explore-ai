package com.ai.billing.service;

import com.ai.billing.domain.model.Plan;
import com.ai.billing.domain.model.QuotaPolicy;
import com.ai.billing.infra.config.BillingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/** Active billing plan and its quota policy. */
@Service
@EnableConfigurationProperties(BillingProperties.class)
@RequiredArgsConstructor
public class BillingPlanService {

  private final BillingProperties properties;

  /** Returns the active plan. */
  public Plan currentPlan() {
    return currentPolicy().getPlan();
  }

  /** Returns the quota policy of the active plan. */
  public QuotaPolicy currentPolicy() {
    return properties.toPolicy();
  }
}
