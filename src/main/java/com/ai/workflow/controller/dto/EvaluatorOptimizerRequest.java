package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluatorOptimizerRequest(
    @NotBlank @Size(max = WorkflowLimits.MAX_TEXT_LENGTH) String task) {}
