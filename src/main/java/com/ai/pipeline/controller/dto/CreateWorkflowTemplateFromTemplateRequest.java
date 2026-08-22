package com.ai.pipeline.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record CreateWorkflowTemplateFromTemplateRequest(@NotBlank String templateId) {}
