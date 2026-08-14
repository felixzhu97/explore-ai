package com.ai.pipeline.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record CreateWorkflowTemplateFromTemplateRequest(@NotBlank String templateId) {}
