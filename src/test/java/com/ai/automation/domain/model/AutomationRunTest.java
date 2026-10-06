package com.ai.automation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.domain.vo.RunStatus;
import com.ai.automation.domain.vo.ScheduleId;
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

    assertThatThrownBy(() -> run.fail("late error", EmailDeliveryStatus.SKIPPED))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> run.skip("late skip")).isInstanceOf(IllegalStateException.class);
    assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
  }
}
