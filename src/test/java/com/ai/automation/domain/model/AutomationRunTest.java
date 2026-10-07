package com.ai.automation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutomationRun")
class AutomationRunTest {

  private static final String OWNER = "c:11111111-1111-1111-1111-111111111111";

  @Test
  @DisplayName("should finish once when the run succeeds")
  void shouldFinishOnceWhenTheRunSucceeds() {
    AutomationRun run = AutomationRun.start(ScheduleId.generate(), OWNER);

    run.succeed("done", EmailDeliveryStatus.SENT);

    assertThat(run.isFinished()).isTrue();
    assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
  }

  @Test
  @DisplayName("should reject a second outcome when the run already finished")
  void shouldRejectASecondOutcomeWhenTheRunAlreadyFinished() {
    AutomationRun run = AutomationRun.start(ScheduleId.generate(), OWNER);
    run.succeed("done", EmailDeliveryStatus.SENT);

    assertThatThrownBy(() -> run.failBeforeEmail("late error"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(run::skipForQuota).isInstanceOf(IllegalStateException.class);
    assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
  }

  @Test
  @DisplayName("should record a skipped run without email when the quota is used up")
  void shouldRecordASkippedRunWithoutEmailWhenTheQuotaIsUsedUp() {
    AutomationRun run = AutomationRun.start(ScheduleId.generate(), OWNER);

    run.skipForQuota();

    assertThat(run.getStatus()).isEqualTo(RunStatus.SKIPPED);
    assertThat(run.getEmailStatus()).isEqualTo(EmailDeliveryStatus.SKIPPED);
    assertThat(run.getErrorMessage()).isEqualTo("Daily plan quota exceeded");
    assertThat(run.isFinished()).isTrue();
  }

  @Test
  @DisplayName("should record a failed run without email when the pipeline fails")
  void shouldRecordAFailedRunWithoutEmailWhenThePipelineFails() {
    AutomationRun run = AutomationRun.start(ScheduleId.generate(), OWNER);

    run.failBeforeEmail("  model timed out ");

    assertThat(run.getStatus()).isEqualTo(RunStatus.FAILED);
    assertThat(run.getEmailStatus()).isEqualTo(EmailDeliveryStatus.SKIPPED);
    assertThat(run.getErrorMessage()).isEqualTo("model timed out");
  }
}
