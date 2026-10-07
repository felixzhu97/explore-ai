package com.ai.automation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AutomationSchedule")
class AutomationScheduleTest {

  private static final String OWNER = "c:client-1";
  private static final String TEMPLATE_ID = "11111111-1111-1111-1111-111111111111";
  private static final Instant NOW = Instant.parse("2026-08-07T00:00:00Z");

  private static AutomationSchedule cronSchedule() {
    return AutomationSchedule.create(
        OWNER,
        "Daily research",
        "0 0 * * * *",
        "Asia/Shanghai",
        TEMPLATE_ID,
        "User@Example.com",
        "Summarize market moves",
        NOW);
  }

  private static AutomationSchedule onceSchedule(Instant runAt) {
    return AutomationSchedule.createOnce(
        OWNER, "One shot", "UTC", TEMPLATE_ID, "user@example.com", "Do once", runAt, NOW);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should arm the first cron run when a cron schedule is created")
    void shouldArmTheFirstCronRunWhenACronScheduleIsCreated() {
      AutomationSchedule schedule = cronSchedule();

      assertThat(schedule.isEnabled()).isTrue();
      assertThat(schedule.getTiming().getScheduleKind()).isEqualTo(ScheduleKind.CRON);
      assertThat(schedule.getActionType()).isEqualTo(AutomationActionType.RUN_PIPELINE_TEMPLATE);
      assertThat(schedule.getRecipientEmail()).isEqualTo("user@example.com");
      assertThat(schedule.getNextRunAt()).isEqualTo(Instant.parse("2026-08-07T01:00:00Z"));
    }

    @Test
    @DisplayName("should reject the schedule when the email is invalid")
    void shouldRejectTheScheduleWhenTheEmailIsInvalid() {
      assertThatThrownBy(
              () ->
                  AutomationSchedule.create(
                      OWNER,
                      "Daily research",
                      "0 0 9 * * *",
                      "Asia/Shanghai",
                      TEMPLATE_ID,
                      "not-an-email",
                      "Summarize market moves",
                      NOW))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("email");
    }

    @Test
    @DisplayName("should keep the run time when a one-off schedule is created")
    void shouldKeepTheRunTimeWhenAOneOffScheduleIsCreated() {
      Instant runAt = NOW.plusSeconds(300);

      AutomationSchedule schedule = onceSchedule(runAt);

      assertThat(schedule.isOnce()).isTrue();
      assertThat(schedule.getTiming().getCronExpression()).isNull();
      assertThat(schedule.getNextRunAt()).isEqualTo(runAt);
      assertThat(schedule.pendingRunAt()).contains(runAt);
    }

    @Test
    @DisplayName("should reject a one-off schedule when runAt is in the past")
    void shouldRejectAOneOffScheduleWhenRunAtIsInThePast() {
      assertThatThrownBy(() -> onceSchedule(NOW.minusSeconds(10)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("runAt");
    }

    @Test
    @DisplayName("should reject a one-off schedule when runAt is missing")
    void shouldRejectAOneOffScheduleWhenRunAtIsMissing() {
      assertThatThrownBy(() -> onceSchedule(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("runAt is required for ONCE schedules");
    }
  }

  @Nested
  @DisplayName("turnOn")
  class TurnOn {

    @Test
    @DisplayName("should re-arm the next cron run when a disabled schedule is turned on")
    void shouldReArmTheNextCronRunWhenADisabledScheduleIsTurnedOn() {
      AutomationSchedule schedule = cronSchedule();
      schedule.disable();
      Instant later = NOW.plus(Duration.ofDays(1));

      schedule.turnOn(later);

      assertThat(schedule.isEnabled()).isTrue();
      assertThat(schedule.getNextRunAt()).isEqualTo(later.plus(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("should reject turning on a one-off schedule when its run already happened")
    void shouldRejectTurningOnAOneOffScheduleWhenItsRunAlreadyHappened() {
      AutomationSchedule schedule = onceSchedule(NOW.plusSeconds(60));
      schedule.recordRunFinished(NOW.plusSeconds(90));

      assertThatThrownBy(() -> schedule.turnOn(NOW.plusSeconds(120)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already completed");
      assertThat(schedule.isEnabled()).isFalse();
    }
  }

  @Nested
  @DisplayName("runs")
  class Runs {

    @Test
    @DisplayName("should claim a one-off run with no further run when it is picked up")
    void shouldClaimAOneOffRunWithNoFurtherRunWhenItIsPickedUp() {
      AutomationSchedule schedule = onceSchedule(NOW.plusSeconds(60));

      Instant provisional = schedule.provisionalNextRunAt(NOW.plusSeconds(60));

      assertThat(provisional).isAfter(NOW.plus(Duration.ofDays(365 * 1000L)));
    }

    @Test
    @DisplayName("should claim the next cron run when a cron schedule is picked up")
    void shouldClaimTheNextCronRunWhenACronScheduleIsPickedUp() {
      AutomationSchedule schedule = cronSchedule();

      assertThat(schedule.provisionalNextRunAt(NOW))
          .isEqualTo(Instant.parse("2026-08-07T01:00:00Z"));
    }

    @Test
    @DisplayName("should turn off a one-off schedule when its run finishes")
    void shouldTurnOffAOneOffScheduleWhenItsRunFinishes() {
      AutomationSchedule schedule = onceSchedule(NOW.plusSeconds(60));
      Instant finished = NOW.plusSeconds(90);

      schedule.recordRunFinished(finished);

      assertThat(schedule.isEnabled()).isFalse();
      assertThat(schedule.getLastRunAt()).isEqualTo(finished);
      assertThat(schedule.pendingRunAt()).isEmpty();
      assertThat(schedule.hasPendingRun(finished)).isFalse();
    }

    @Test
    @DisplayName("should move a cron schedule to its next run when a run finishes")
    void shouldMoveACronScheduleToItsNextRunWhenARunFinishes() {
      AutomationSchedule schedule = cronSchedule();
      Instant finished = Instant.parse("2026-08-07T01:05:00Z");

      schedule.recordRunFinished(finished);

      assertThat(schedule.isEnabled()).isTrue();
      assertThat(schedule.getLastRunAt()).isEqualTo(finished);
      assertThat(schedule.getNextRunAt()).isEqualTo(Instant.parse("2026-08-07T02:00:00Z"));
      assertThat(schedule.hasPendingRun(finished)).isTrue();
    }

    @Test
    @DisplayName("should address the result email to the recipient with the schedule name")
    void shouldAddressTheResultEmailToTheRecipientWithTheScheduleName() {
      EmailMessage email = cronSchedule().resultEmail("text", "<p>html</p>");

      assertThat(email.to()).isEqualTo("user@example.com");
      assertThat(email.subject()).isEqualTo("[ExploreAI] Daily research");
      assertThat(email.htmlBody()).isEqualTo("<p>html</p>");
    }
  }

  @Test
  @DisplayName("should turn a finished one-off schedule back on when it gets a new run time")
  void shouldTurnAFinishedOneOffScheduleBackOnWhenItGetsANewRunTime() {
    AutomationSchedule schedule = onceSchedule(NOW.plusSeconds(60));
    schedule.recordRunFinished(NOW.plusSeconds(90));
    Instant newRunAt = NOW.plusSeconds(600);

    schedule.update(
        "One shot again",
        ScheduleTiming.once("UTC"),
        newRunAt,
        TEMPLATE_ID,
        "user@example.com",
        "Do once again",
        NOW.plusSeconds(120));

    assertThat(schedule.isEnabled()).isTrue();
    assertThat(schedule.getNextRunAt()).isEqualTo(newRunAt);
    assertThat(schedule.getName()).isEqualTo("One shot again");
  }
}
