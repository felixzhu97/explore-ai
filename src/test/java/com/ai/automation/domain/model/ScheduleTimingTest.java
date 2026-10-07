package com.ai.automation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScheduleTiming")
class ScheduleTimingTest {

  @Test
  @DisplayName("should trim the cron expression and keep the time zone when timing is cron")
  void shouldTrimTheCronExpressionAndKeepTheTimeZoneWhenTimingIsCron() {
    ScheduleTiming timing = ScheduleTiming.createCronTiming(" 0 0 9 * * * ", "Asia/Shanghai");

    assertThat(timing.getScheduleKind()).isEqualTo(ScheduleKind.CRON);
    assertThat(timing.getCronExpression()).isEqualTo("0 0 9 * * *");
    assertThat(timing.getTimezone()).isEqualTo("Asia/Shanghai");
    assertThat(timing.isOneOff()).isFalse();
  }

  @Test
  @DisplayName("should drop the cron expression when timing is a one-off run")
  void shouldDropTheCronExpressionWhenTimingIsAOneOffRun() {
    ScheduleTiming timing = ScheduleTiming.createTiming(ScheduleKind.ONCE, "0 0 9 * * *", "UTC");

    assertThat(timing.isOneOff()).isTrue();
    assertThat(timing.getCronExpression()).isNull();
  }

  @Test
  @DisplayName("should reject the timing when the time zone is unknown")
  void shouldRejectTheTimingWhenTheTimeZoneIsUnknown() {
    assertThatThrownBy(() -> ScheduleTiming.createOneOffTiming("Mars/Olympus"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("timezone");
  }

  @Test
  @DisplayName("should reject a cron timing when the expression is blank")
  void shouldRejectACronTimingWhenTheExpressionIsBlank() {
    assertThatThrownBy(() -> ScheduleTiming.createCronTiming("  ", "UTC"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should return the next fire time when the cron expression is valid")
  void shouldReturnTheNextFireTimeWhenTheCronExpressionIsValid() {
    ScheduleTiming timing = ScheduleTiming.createCronTiming("0 0 9 * * *", "UTC");

    assertThat(timing.calculateNextRunAt(Instant.parse("2026-08-06T00:00:00Z")))
        .isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
  }

  @Test
  @DisplayName("should read the fire time in the schedule time zone")
  void shouldReadTheFireTimeInTheScheduleTimeZone() {
    ScheduleTiming timing = ScheduleTiming.createCronTiming("0 0 9 * * *", "Asia/Shanghai");

    assertThat(timing.calculateNextRunAt(Instant.parse("2026-08-06T00:00:00Z")))
        .isEqualTo(Instant.parse("2026-08-06T01:00:00Z"));
  }

  @Test
  @DisplayName("should reject the timing when the expression is not a valid cron")
  void shouldRejectTheTimingWhenTheExpressionIsNotAValidCron() {
    assertThatThrownBy(() -> ScheduleTiming.createCronTiming("not-a-cron", "UTC"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should have no cron fire time when the timing is one-off")
  void shouldHaveNoCronFireTimeWhenTheTimingIsOneOff() {
    assertThatThrownBy(
            () -> ScheduleTiming.createOneOffTiming("UTC").calculateNextRunAt(Instant.now()))
        .isInstanceOf(IllegalStateException.class);
  }
}
