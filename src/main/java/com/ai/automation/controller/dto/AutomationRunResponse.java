package com.ai.automation.controller.dto;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.domain.vo.RunStatus;
import java.time.Instant;

public record AutomationRunResponse(
    String id,
    String scheduleId,
    Instant startedAt,
    Instant finishedAt,
    RunStatus status,
    String errorMessage,
    String resultExcerpt,
    EmailDeliveryStatus emailStatus) {
  /** Builds a response from an automation run record. */
  public static AutomationRunResponse from(AutomationRun run) {
    return new AutomationRunResponse(
        run.getId().value(),
        run.getScheduleId().value(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getStatus(),
        run.getErrorMessage(),
        run.getResultExcerpt(),
        run.getEmailStatus());
  }
}
