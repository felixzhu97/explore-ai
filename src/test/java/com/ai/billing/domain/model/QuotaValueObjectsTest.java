package com.ai.billing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.domain.model.OwnerKey;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Quota value objects")
class QuotaValueObjectsTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

  @Test
  @DisplayName("should fall back to free when the configured plan is unknown")
  void shouldFallBackToFreeWhenTheConfiguredPlanIsUnknown() {
    assertThat(Plan.parsePlan(" Pro ")).isEqualTo(Plan.PRO);
    assertThat(Plan.parsePlan("enterprise")).isEqualTo(Plan.FREE);
    assertThat(Plan.parsePlan(null)).isEqualTo(Plan.FREE);
  }

  @Test
  @DisplayName("should keep the ip limit at least as high as the plan limit")
  void shouldKeepTheIpLimitAtLeastAsHighAsThePlanLimit() {
    QuotaPolicy policy = new QuotaPolicy(true, Plan.PRO, 500, 200, 0);

    assertThat(policy.ipDailyLimit()).isEqualTo(500);
  }

  @Test
  @DisplayName("should turn the global limit off when it is zero")
  void shouldTurnTheGlobalLimitOffWhenItIsZero() {
    assertThat(new QuotaPolicy(true, Plan.FREE, 50, 200, 0).globalDailyLimit()).isEmpty();
    assertThat(new QuotaPolicy(true, Plan.FREE, 50, 200, 9).globalDailyLimit()).hasValue(9);
  }

  @Test
  @DisplayName("should reject negative limits when building a policy")
  void shouldRejectNegativeLimitsWhenBuildingAPolicy() {
    assertThatThrownBy(() -> new QuotaPolicy(true, Plan.FREE, -1, 200, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should start from zero when the day changes")
  void shouldStartFromZeroWhenTheDayChanges() {
    DailyUsage yesterday = new DailyUsage(TODAY.minusDays(1), 5);

    assertThat(yesterday.calculateRemaining(5, TODAY)).isEqualTo(5);
    assertThat(yesterday.consumeQuota(TODAY)).isEqualTo(new DailyUsage(TODAY, 1));
  }

  @Test
  @DisplayName("should give back a consumed request when released")
  void shouldGiveBackAConsumedRequestWhenReleased() {
    DailyUsage usage = DailyUsage.createEmptyUsage(TODAY).consumeQuota(TODAY).consumeQuota(TODAY);

    assertThat(usage.calculateRemaining(3, TODAY)).isEqualTo(1);
    assertThat(usage.release(TODAY).calculateRemaining(3, TODAY)).isEqualTo(2);
    assertThat(DailyUsage.createEmptyUsage(TODAY).release(TODAY).getCount()).isZero();
  }

  @Test
  @DisplayName("should keep owner and address counters apart when the values look alike")
  void shouldKeepOwnerAndAddressCountersApartWhenTheValuesLookAlike() {
    QuotaSubject owner = QuotaSubject.createOwnerSubject(OwnerKey.createClientKey("203.0.113.7"));
    QuotaSubject address = QuotaSubject.createAddressSubject("203.0.113.7");

    assertThat(owner).isNotEqualTo(address).isNotEqualTo(QuotaSubject.GLOBAL);
    assertThat(address.toString()).doesNotContain("203.0.113.7");
  }

  @Test
  @DisplayName("should have nothing remaining when a decision refuses the request")
  void shouldHaveNothingRemainingWhenADecisionRefusesTheRequest() {
    assertThat(QuotaDecision.createRefusal(Plan.FREE, 50).getRemaining()).isZero();
    assertThatThrownBy(() -> new QuotaDecision(false, Plan.FREE, 50, 3))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
