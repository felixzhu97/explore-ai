package com.ai.automation.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAutomationScheduleRequest(
        @NotBlank String name,
        @NotBlank String cronExpression,
        @NotBlank String timezone,
        @NotBlank String workflowTemplateId,
        @NotBlank @Email String recipientEmail,
        @NotBlank String brief) {
}
