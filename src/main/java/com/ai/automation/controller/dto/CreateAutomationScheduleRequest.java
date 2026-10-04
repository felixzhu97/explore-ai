package com.ai.automation.controller.dto;

import com.ai.automation.domain.vo.ScheduleKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAutomationScheduleRequest(
    @NotBlank String name,
    @NotNull ScheduleKind scheduleKind,
    String cronExpression,
    Instant runAt,
    @NotBlank String timezone,
    @NotBlank String pipelineTemplateId,
    @NotBlank @Email String recipientEmail,
    @NotBlank String brief) {}
