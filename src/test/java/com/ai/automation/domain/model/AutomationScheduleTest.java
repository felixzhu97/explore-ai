package com.ai.automation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutomationSchedule")
class AutomationScheduleTest {

    @Test
    void should_createEnabledSchedule_when_inputsValid() {
        Instant next = Instant.parse("2026-08-07T01:00:00Z");
        AutomationSchedule schedule = AutomationSchedule.create(
                "client-1",
                "Daily research",
                "0 0 9 * * *",
                "Asia/Shanghai",
                "11111111-1111-1111-1111-111111111111",
                "user@example.com",
                "Summarize market moves",
                next);

        assertThat(schedule.isEnabled()).isTrue();
        assertThat(schedule.getRecipientEmail()).isEqualTo("user@example.com");
        assertThat(schedule.getNextRunAt()).isEqualTo(next);
    }

    @Test
    void should_rejectInvalidEmail_when_create() {
        assertThatThrownBy(() -> AutomationSchedule.create(
                "client-1",
                "Daily research",
                "0 0 9 * * *",
                "Asia/Shanghai",
                "11111111-1111-1111-1111-111111111111",
                "not-an-email",
                "Summarize market moves",
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void should_disableSchedule_when_disableCalled() {
        AutomationSchedule schedule = AutomationSchedule.create(
                "client-1",
                "Daily research",
                "0 0 9 * * *",
                "Asia/Shanghai",
                "11111111-1111-1111-1111-111111111111",
                "user@example.com",
                "Summarize market moves",
                Instant.now());

        schedule.disable();

        assertThat(schedule.isEnabled()).isFalse();
    }
}
