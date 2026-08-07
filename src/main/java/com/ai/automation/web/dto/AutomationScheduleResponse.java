package com.ai.automation.web.dto;

import com.ai.automation.domain.model.AutomationSchedule;

import java.time.Instant;

public record AutomationScheduleResponse(
        String id,
        String name,
        String cronExpression,
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

    public static AutomationScheduleResponse from(AutomationSchedule schedule) {
        return new AutomationScheduleResponse(
                schedule.getId().value(),
                schedule.getName(),
                schedule.getCronExpression(),
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
