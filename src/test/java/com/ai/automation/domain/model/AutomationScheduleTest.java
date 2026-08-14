package com.ai.automation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.automation.domain.vo.ScheduleKind;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutomationSchedule")
class AutomationScheduleTest {

  @Test
  void shouldCreateEnabledScheduleWhenInputsValid() {
    Instant next = Instant.parse("2026-08-07T01:00:00Z");
    AutomationSchedule schedule =
        AutomationSchedule.create(
            "client-1",
            "Daily research",
            "0 0 9 * * *",
            "Asia/Shanghai",
            "11111111-1111-1111-1111-111111111111",
            "user@example.com",
            "Summarize market moves",
            next);

    assertThat(schedule.isEnabled()).isTrue();
    assertThat(schedule.getScheduleKind()).isEqualTo(ScheduleKind.CRON);
    assertThat(schedule.getRecipientEmail()).isEqualTo("user@example.com");
    assertThat(schedule.getNextRunAt()).isEqualTo(next);
  }

  @Test
  void shouldRejectInvalidEmailWhenCreate() {
    assertThatThrownBy(
            () ->
                AutomationSchedule.create(
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
  void shouldDisableScheduleWhenDisableCalled() {
    AutomationSchedule schedule =
        AutomationSchedule.create(
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

  @Test
  void shouldCreateOnceScheduleWhenRunAtInFuture() {
    Instant runAt = Instant.now().plus(5, ChronoUnit.MINUTES);
    AutomationSchedule schedule =
        AutomationSchedule.createOnce(
            "client-1",
            "One shot",
            "Asia/Shanghai",
            "11111111-1111-1111-1111-111111111111",
            "user@example.com",
            "Do once",
            runAt);

    assertThat(schedule.isOnce()).isTrue();
    assertThat(schedule.getCronExpression()).isNull();
    assertThat(schedule.getNextRunAt()).isEqualTo(runAt);
  }

  @Test
  void shouldRejectPastRunAtWhenCreateOnce() {
    assertThatThrownBy(
            () ->
                AutomationSchedule.createOnce(
                    "client-1",
                    "One shot",
                    "Asia/Shanghai",
                    "11111111-1111-1111-1111-111111111111",
                    "user@example.com",
                    "Do once",
                    Instant.now().minusSeconds(10)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("runAt");
  }

  @Test
  void shouldDisableAfterCompleteOnce() {
    AutomationSchedule schedule =
        AutomationSchedule.createOnce(
            "client-1",
            "One shot",
            "UTC",
            "11111111-1111-1111-1111-111111111111",
            "user@example.com",
            "Do once",
            Instant.now().plusSeconds(60));

    schedule.completeOnce(Instant.now());

    assertThat(schedule.isEnabled()).isFalse();
    assertThat(schedule.getNextRunAt()).isEqualTo(AutomationSchedule.ONCE_TERMINAL_NEXT);
    assertThat(schedule.getLastRunAt()).isNotNull();
  }

  @Test
  void shouldReenableWhenUpdateOnceAfterComplete() {
    AutomationSchedule schedule =
        AutomationSchedule.createOnce(
            "client-1",
            "One shot",
            "UTC",
            "11111111-1111-1111-1111-111111111111",
            "user@example.com",
            "Do once",
            Instant.now().plusSeconds(60));
    schedule.completeOnce(Instant.now());

    Instant newRunAt = Instant.now().plus(10, ChronoUnit.MINUTES);
    schedule.update(
        "One shot again",
        ScheduleKind.ONCE,
        null,
        "UTC",
        "11111111-1111-1111-1111-111111111111",
        "user@example.com",
        "Do once again",
        newRunAt);

    assertThat(schedule.isEnabled()).isTrue();
    assertThat(schedule.getNextRunAt()).isEqualTo(newRunAt);
    assertThat(schedule.getName()).isEqualTo("One shot again");
  }
}
