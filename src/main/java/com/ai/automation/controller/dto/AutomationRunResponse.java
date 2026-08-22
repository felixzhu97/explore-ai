package com.ai.automation.controller.dto;

import com.ai.automation.domain.model.AutomationRun;
import java.time.Instant;

/** Documentation. */
public record AutomationRunResponse(
    String id,
    String scheduleId,
    Instant startedAt,
    Instant finishedAt,
    String status,
    String errorMessage,
    String resultExcerpt,
    String emailStatus) {
  /** Documentation. */
  public static AutomationRunResponse from(AutomationRun run) {
    return new AutomationRunResponse(
        run.getId().value(),
        run.getScheduleId().value(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getStatus().value(),
        run.getErrorMessage(),
        run.getResultExcerpt(),
        run.getEmailStatus().value());
  }
}
