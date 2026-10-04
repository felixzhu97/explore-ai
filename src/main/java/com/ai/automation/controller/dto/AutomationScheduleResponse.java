package com.ai.automation.controller.dto;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.AutomationActionType;
import com.ai.automation.domain.vo.ScheduleKind;
import java.time.Instant;

public record AutomationScheduleResponse(
    String id,
    String name,
    ScheduleKind scheduleKind,
    String cronExpression,
    Instant runAt,
    String timezone,
    boolean enabled,
    AutomationActionType actionType,
    String pipelineTemplateId,
    String recipientEmail,
    String brief,
    Instant nextRunAt,
    Instant lastRunAt,
    Instant createdAt,
    Instant updatedAt) {
  /** Builds a response from a schedule, exposing {@code runAt} only for pending one-off runs. */
  public static AutomationScheduleResponse from(AutomationSchedule schedule) {
    Instant runAt = null;
    if (schedule.getScheduleKind() == ScheduleKind.ONCE
        && !AutomationSchedule.ONCE_TERMINAL_NEXT.equals(schedule.getNextRunAt())) {
      runAt = schedule.getNextRunAt();
    }
    return new AutomationScheduleResponse(
        schedule.getId().value(),
        schedule.getName(),
        schedule.getScheduleKind(),
        schedule.getCronExpression(),
        runAt,
        schedule.getTimezone(),
        schedule.isEnabled(),
        schedule.getActionType(),
        schedule.getPipelineTemplateId().value(),
        schedule.getRecipientEmail(),
        schedule.getBrief(),
        schedule.getNextRunAt(),
        schedule.getLastRunAt(),
        schedule.getCreatedAt(),
        schedule.getUpdatedAt());
  }
}
