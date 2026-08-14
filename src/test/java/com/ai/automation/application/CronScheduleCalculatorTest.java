package com.ai.automation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CronScheduleCalculator")
class CronScheduleCalculatorTest {

  private final CronScheduleCalculator calculator = new CronScheduleCalculator();

  @Test
  void shouldComputeNextRunWhenCronValid() {
    Instant after = Instant.parse("2026-08-06T00:00:00Z");
    Instant next = calculator.nextRunAt("0 0 9 * * *", "UTC", after);

    assertThat(next).isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
  }

  @Test
  void shouldRejectInvalidCronWhenValidate() {
    assertThatThrownBy(() -> calculator.validate("not-a-cron", "UTC"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
