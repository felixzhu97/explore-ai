package com.ai.automation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScheduleTiming")
class ScheduleTimingTest {

  @Test
  @DisplayName("should trim the cron expression and keep the time zone when timing is cron")
  void shouldTrimTheCronExpressionAndKeepTheTimeZoneWhenTimingIsCron() {
    ScheduleTiming timing = ScheduleTiming.cron(" 0 0 9 * * * ", "Asia/Shanghai");

    assertThat(timing.getScheduleKind()).isEqualTo(ScheduleKind.CRON);
    assertThat(timing.getCronExpression()).isEqualTo("0 0 9 * * *");
    assertThat(timing.getTimezone()).isEqualTo("Asia/Shanghai");
    assertThat(timing.isOnce()).isFalse();
  }

  @Test
  @DisplayName("should drop the cron expression when timing is a one-off run")
  void shouldDropTheCronExpressionWhenTimingIsAOneOffRun() {
    ScheduleTiming timing = ScheduleTiming.of(ScheduleKind.ONCE, "0 0 9 * * *", "UTC");

    assertThat(timing.isOnce()).isTrue();
    assertThat(timing.getCronExpression()).isNull();
  }

  @Test
  @DisplayName("should reject the timing when the time zone is unknown")
  void shouldRejectTheTimingWhenTheTimeZoneIsUnknown() {
    assertThatThrownBy(() -> ScheduleTiming.once("Mars/Olympus"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("timezone");
  }

  @Test
  @DisplayName("should reject a cron timing when the expression is blank")
  void shouldRejectACronTimingWhenTheExpressionIsBlank() {
    assertThatThrownBy(() -> ScheduleTiming.cron("  ", "UTC"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
