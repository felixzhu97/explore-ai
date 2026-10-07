package com.ai.billing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.billing.domain.model.Plan;
import com.ai.billing.domain.model.QuotaDecision;
import com.ai.billing.domain.model.QuotaSubject;
import com.ai.billing.infra.config.BillingProperties;
import com.ai.common.domain.model.OwnerKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DailyUsageQuotaService")
class DailyUsageQuotaServiceTest {

  private BillingProperties properties;
  private DailyUsageQuotaService service;

  @BeforeEach
  void setUp() {
    properties = new BillingProperties();
    properties.setFreeDailyRequests(2);
    properties.setIpDailyRequests(3);
    properties.setGlobalDailyRequests(0);
    service = new DailyUsageQuotaService(new BillingPlanService(properties));
  }

  @Test
  @DisplayName("should reject new client ids when they share an exhausted ip")
  void shouldRejectNewClientIdsWhenTheyShareAnExhaustedIp() {
    assertThat(consume("client-a", "203.0.113.7").isAllowed()).isTrue();
    assertThat(consume("client-b", "203.0.113.7").isAllowed()).isTrue();
    assertThat(consume("client-c", "203.0.113.7").isAllowed()).isTrue();

    assertThat(consume("client-d", "203.0.113.7").isAllowed()).isFalse();
    assertThat(consume("client-d", "198.51.100.9").isAllowed()).isTrue();
  }

  @Test
  @DisplayName("should not consume other limits when one limit is exhausted")
  void shouldNotConsumeOtherLimitsWhenOneLimitIsExhausted() {
    consume("client-a", "203.0.113.7");
    consume("client-a", "203.0.113.7");

    assertThat(consume("client-a", "203.0.113.7").isAllowed()).isFalse();
    assertThat(consume("client-b", "203.0.113.7").isAllowed()).isTrue();
  }

  @Test
  @DisplayName("should report the owner's remaining requests when allowed")
  void shouldReportTheOwnersRemainingRequestsWhenAllowed() {
    QuotaDecision decision = consume("client-a", "203.0.113.7");

    assertThat(decision).isEqualTo(QuotaDecision.createApproval(Plan.FREE, 2, 1));
  }

  @Test
  @DisplayName("should count against the ip limit when the request has no client identity")
  void shouldCountAgainstTheIpLimitWhenTheRequestHasNoClientIdentity() {
    assertThat(service.tryConsume(null, "203.0.113.7").getRemaining()).isEqualTo(2);
    assertThat(service.tryConsume(null, "203.0.113.7").getRemaining()).isEqualTo(1);
    assertThat(service.tryConsume(null, "203.0.113.7").getRemaining()).isZero();

    assertThat(service.tryConsume(null, "203.0.113.7").isAllowed()).isFalse();
  }

  @Test
  @DisplayName("should share the guest counter between requests and automations")
  void shouldShareTheGuestCounterBetweenRequestsAndAutomations() {
    OwnerKey guest = OwnerKey.createClientKey("client-a");

    assertThat(service.tryConsume(guest)).isTrue();
    assertThat(consume("client-a", null).getRemaining()).isZero();
    assertThat(service.tryConsume(guest)).isFalse();
  }

  @Test
  @DisplayName("should skip ip limit when address is null")
  void shouldSkipIpLimitWhenAddressIsNull() {
    properties.setFreeDailyRequests(10);
    properties.setIpDailyRequests(1);

    assertThat(consume("client-a", null).isAllowed()).isTrue();
    assertThat(consume("client-a", null).isAllowed()).isTrue();
  }

  @Test
  @DisplayName("should allow everything when the quota is not enforced")
  void shouldAllowEverythingWhenTheQuotaIsNotEnforced() {
    properties.setQuotaEnabled(false);
    properties.setFreeDailyRequests(1);

    consume("client-a", null);

    assertThat(consume("client-a", null)).isEqualTo(QuotaDecision.createApproval(Plan.FREE, 1, 1));
  }

  private QuotaDecision consume(String clientId, String address) {
    return service.tryConsume(
        QuotaSubject.createOwnerSubject(OwnerKey.createClientKey(clientId)), address);
  }
}
