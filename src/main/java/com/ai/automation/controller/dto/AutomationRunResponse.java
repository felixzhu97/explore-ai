package com.ai.automation.controller.dto;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.EmailDeliveryStatus;
import com.ai.automation.domain.model.RunStatus;
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
  public static AutomationRunResponse createResponse(AutomationRun run) {
    return new AutomationRunResponse(
        run.getId().toString(),
        run.getScheduleId().toString(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getStatus(),
        run.getErrorMessage(),
        run.getResultExcerpt(),
        run.getEmailStatus());
  }
}
