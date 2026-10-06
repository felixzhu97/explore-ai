package com.ai.billing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.billing.infra.config.BillingProperties;
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
    service = new DailyUsageQuotaService(properties);
  }

  @Test
  @DisplayName("should reject new client ids when they share an exhausted ip")
  void shouldRejectNewClientIdsWhenTheyShareAnExhaustedIp() {
    assertThat(service.tryConsume("client-a", "203.0.113.7")).isTrue();
    assertThat(service.tryConsume("client-b", "203.0.113.7")).isTrue();
    assertThat(service.tryConsume("client-c", "203.0.113.7")).isTrue();

    assertThat(service.tryConsume("client-d", "203.0.113.7")).isFalse();
    assertThat(service.tryConsume("client-d", "198.51.100.9")).isTrue();
  }

  @Test
  @DisplayName("should not consume other limits when one limit is exhausted")
  void shouldNotConsumeOtherLimitsWhenOneLimitIsExhausted() {
    service.tryConsume("client-a", "203.0.113.7");
    service.tryConsume("client-a", "203.0.113.7");

    assertThat(service.tryConsume("client-a", "203.0.113.7")).isFalse();
    assertThat(service.tryConsume("client-b", "203.0.113.7")).isTrue();
  }

  @Test
  @DisplayName("should reject every client when global daily limit is reached")
  void shouldRejectEveryClientWhenGlobalDailyLimitIsReached() {
    properties.setGlobalDailyRequests(2);

    assertThat(service.tryConsume("client-a", "203.0.113.1")).isTrue();
    assertThat(service.tryConsume("client-b", "203.0.113.2")).isTrue();

    assertThat(service.tryConsume("client-c", "203.0.113.3")).isFalse();
    assertThat(service.countRemaining("client-c")).isEqualTo(2);
  }

  @Test
  @DisplayName("should skip ip limit when address is null")
  void shouldSkipIpLimitWhenAddressIsNull() {
    properties.setFreeDailyRequests(10);
    properties.setIpDailyRequests(1);

    assertThat(service.tryConsume("client-a")).isTrue();
    assertThat(service.tryConsume("client-a")).isTrue();
  }

  @Test
  @DisplayName("should keep ip limit at least as high as plan limit")
  void shouldKeepIpLimitAtLeastAsHighAsPlanLimit() {
    properties.setPlan("pro");
    properties.setProDailyRequests(500);

    assertThat(properties.resolveIpDailyLimit()).isEqualTo(500);
  }
}
