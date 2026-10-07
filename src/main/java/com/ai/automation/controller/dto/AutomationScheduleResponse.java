package com.ai.automation.controller.dto;

import com.ai.automation.domain.model.AutomationActionType;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.ScheduleKind;
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
    return new AutomationScheduleResponse(
        schedule.getId().toString(),
        schedule.getName(),
        schedule.getTiming().getScheduleKind(),
        schedule.getTiming().getCronExpression(),
        schedule.pendingRunAt().orElse(null),
        schedule.getTiming().getTimezone(),
        schedule.isEnabled(),
        schedule.getActionType(),
        schedule.getPipelineTemplateId().toString(),
        schedule.getRecipientEmail(),
        schedule.getBrief(),
        schedule.getNextRunAt(),
        schedule.getLastRunAt(),
        schedule.getCreatedAt(),
        schedule.getUpdatedAt());
  }
}
