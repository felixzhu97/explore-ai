package com.ai.automation.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreateAutomationScheduleRequest(
    @NotBlank String name,
    String scheduleKind,
    String cronExpression,
    Instant runAt,
    @NotBlank String timezone,
    @NotBlank String pipelineTemplateId,
    @NotBlank @Email String recipientEmail,
    @NotBlank String brief) {}
