package com.ai.automation.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** Documentation. */
public record CreateAutomationScheduleRequest(
    @NotBlank String name,
    String scheduleKind,
    String cronExpression,
    Instant runAt,
    @NotBlank String timezone,
    @NotBlank String workflowTemplateId,
    @NotBlank @Email String recipientEmail,
    @NotBlank String brief) {}
