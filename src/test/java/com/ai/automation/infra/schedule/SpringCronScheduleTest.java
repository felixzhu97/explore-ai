package com.ai.automation.infra.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpringCronSchedule")
class SpringCronScheduleTest {

  private final SpringCronSchedule cronSchedule = new SpringCronSchedule();

  @Test
  @DisplayName("should return the next fire time when the cron expression is valid")
  void shouldReturnTheNextFireTimeWhenTheCronExpressionIsValid() {
    Instant after = Instant.parse("2026-08-06T00:00:00Z");

    assertThat(cronSchedule.nextRunAfter("0 0 9 * * *", "UTC", after))
        .isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
  }

  @Test
  @DisplayName("should read the fire time in the schedule time zone")
  void shouldReadTheFireTimeInTheScheduleTimeZone() {
    Instant after = Instant.parse("2026-08-06T00:00:00Z");

    assertThat(cronSchedule.nextRunAfter("0 0 9 * * *", "Asia/Shanghai", after))
        .isEqualTo(Instant.parse("2026-08-06T01:00:00Z"));
  }

  @Test
  @DisplayName("should reject the expression when it is not a valid cron")
  void shouldRejectTheExpressionWhenItIsNotAValidCron() {
    assertThatThrownBy(() -> cronSchedule.nextRunAfter("not-a-cron", "UTC", Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
