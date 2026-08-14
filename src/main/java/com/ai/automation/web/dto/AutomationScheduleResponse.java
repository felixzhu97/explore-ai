package com.ai.automation.web.dto;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.ScheduleKind;
import java.time.Instant;

/** Documentation. */
public record AutomationScheduleResponse(
    String id,
    String name,
    String scheduleKind,
    String cronExpression,
    Instant runAt,
    String timezone,
    boolean enabled,
    String actionType,
    String workflowTemplateId,
    String recipientEmail,
    String brief,
    Instant nextRunAt,
    Instant lastRunAt,
    Instant createdAt,
    Instant updatedAt) {
  /** Documentation. */
  public static AutomationScheduleResponse from(AutomationSchedule schedule) {
    Instant runAt = null;
    if (schedule.getScheduleKind() == ScheduleKind.ONCE
        && !AutomationSchedule.ONCE_TERMINAL_NEXT.equals(schedule.getNextRunAt())) {
      runAt = schedule.getNextRunAt();
    }
    return new AutomationScheduleResponse(
        schedule.getId().value(),
        schedule.getName(),
        schedule.getScheduleKind().value(),
        schedule.getCronExpression(),
        runAt,
        schedule.getTimezone(),
        schedule.isEnabled(),
        schedule.getActionType().value(),
        schedule.getWorkflowTemplateId(),
        schedule.getRecipientEmail(),
        schedule.getBrief(),
        schedule.getNextRunAt(),
        schedule.getLastRunAt(),
        schedule.getCreatedAt(),
        schedule.getUpdatedAt());
  }
}
