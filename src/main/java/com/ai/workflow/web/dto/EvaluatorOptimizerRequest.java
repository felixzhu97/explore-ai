package com.ai.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluatorOptimizerRequest(@NotBlank String task) {
}
