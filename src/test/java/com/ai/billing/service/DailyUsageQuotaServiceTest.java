package com.ai.billing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.billing.domain.vo.Plan;
import com.ai.billing.domain.vo.QuotaDecision;
import com.ai.billing.domain.vo.QuotaSubject;
import com.ai.billing.infra.config.BillingProperties;
import com.ai.common.domain.vo.OwnerKey;
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
    assertThat(consume("client-a", "203.0.113.7").allowed()).isTrue();
    assertThat(consume("client-b", "203.0.113.7").allowed()).isTrue();
    assertThat(consume("client-c", "203.0.113.7").allowed()).isTrue();

    assertThat(consume("client-d", "203.0.113.7").allowed()).isFalse();
    assertThat(consume("client-d", "198.51.100.9").allowed()).isTrue();
  }

  @Test
  @DisplayName("should not consume other limits when one limit is exhausted")
  void shouldNotConsumeOtherLimitsWhenOneLimitIsExhausted() {
    consume("client-a", "203.0.113.7");
    consume("client-a", "203.0.113.7");

    assertThat(consume("client-a", "203.0.113.7").allowed()).isFalse();
    assertThat(consume("client-b", "203.0.113.7").allowed()).isTrue();
  }

  @Test
  @DisplayName("should report nothing remaining when the global limit refuses the request")
  void shouldReportNothingRemainingWhenTheGlobalLimitRefusesTheRequest() {
    properties.setGlobalDailyRequests(2);
    consume("client-a", "203.0.113.1");
    consume("client-b", "203.0.113.2");

    QuotaDecision decision = consume("client-c", "203.0.113.3");

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.limit()).isEqualTo(2);
    assertThat(decision.remaining()).isZero();
  }

  @Test
  @DisplayName("should report the owner's remaining requests when allowed")
  void shouldReportTheOwnersRemainingRequestsWhenAllowed() {
    QuotaDecision decision = consume("client-a", "203.0.113.7");

    assertThat(decision).isEqualTo(QuotaDecision.allow(Plan.FREE, 2, 1));
  }

  @Test
  @DisplayName("should count against the ip limit when the request has no client identity")
  void shouldCountAgainstTheIpLimitWhenTheRequestHasNoClientIdentity() {
    assertThat(service.tryConsume(null, "203.0.113.7").remaining()).isEqualTo(2);
    assertThat(service.tryConsume(null, "203.0.113.7").remaining()).isEqualTo(1);
    assertThat(service.tryConsume(null, "203.0.113.7").remaining()).isZero();

    assertThat(service.tryConsume(null, "203.0.113.7").allowed()).isFalse();
  }

  @Test
  @DisplayName("should share the guest counter between requests and automations")
  void shouldShareTheGuestCounterBetweenRequestsAndAutomations() {
    OwnerKey guest = OwnerKey.forClient("client-a");

    assertThat(service.tryConsume(guest)).isTrue();
    assertThat(consume("client-a", null).remaining()).isZero();
    assertThat(service.tryConsume(guest)).isFalse();
  }

  @Test
  @DisplayName("should skip ip limit when address is null")
  void shouldSkipIpLimitWhenAddressIsNull() {
    properties.setFreeDailyRequests(10);
    properties.setIpDailyRequests(1);

    assertThat(consume("client-a", null).allowed()).isTrue();
    assertThat(consume("client-a", null).allowed()).isTrue();
  }

  @Test
  @DisplayName("should allow everything when the quota is not enforced")
  void shouldAllowEverythingWhenTheQuotaIsNotEnforced() {
    properties.setQuotaEnabled(false);
    properties.setFreeDailyRequests(1);

    consume("client-a", null);

    assertThat(consume("client-a", null)).isEqualTo(QuotaDecision.allow(Plan.FREE, 1, 1));
  }

  private QuotaDecision consume(String clientId, String address) {
    return service.tryConsume(QuotaSubject.owner(OwnerKey.forClient(clientId)), address);
  }
}
