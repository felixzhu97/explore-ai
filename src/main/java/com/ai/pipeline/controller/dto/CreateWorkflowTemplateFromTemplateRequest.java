package com.ai.pipeline.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowTemplateFromTemplateRequest(@NotBlank String templateId) {}
