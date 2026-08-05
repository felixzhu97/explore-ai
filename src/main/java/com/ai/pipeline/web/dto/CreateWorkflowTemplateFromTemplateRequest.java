package com.ai.pipeline.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowTemplateFromTemplateRequest(@NotBlank String templateId) {}
