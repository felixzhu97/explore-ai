package com.ai.automation.web.dto;

import com.ai.automation.domain.model.AutomationRun;

import java.time.Instant;

public record AutomationRunResponse(
        String id,
        String scheduleId,
        Instant startedAt,
        Instant finishedAt,
        String status,
        String errorMessage,
        String resultExcerpt,
        String emailStatus) {

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
