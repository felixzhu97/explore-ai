package com.ai.automation.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CronScheduleCalculator")
class CronScheduleCalculatorTest {

    private final CronScheduleCalculator calculator = new CronScheduleCalculator();

    @Test
    void should_computeNextRun_when_cronValid() {
        Instant after = Instant.parse("2026-08-06T00:00:00Z");
        Instant next = calculator.nextRunAt("0 0 9 * * *", "UTC", after);

        assertThat(next).isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
    }

    @Test
    void should_rejectInvalidCron_when_validate() {
        assertThatThrownBy(() -> calculator.validate("not-a-cron", "UTC"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
