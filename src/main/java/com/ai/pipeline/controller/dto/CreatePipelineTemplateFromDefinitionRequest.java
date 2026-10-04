package com.ai.pipeline.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePipelineTemplateFromDefinitionRequest(@NotBlank String templateId) {}
