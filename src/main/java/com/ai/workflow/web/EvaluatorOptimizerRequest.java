package com.ai.workflow.web;

import jakarta.validation.constraints.NotBlank;

public record EvaluatorOptimizerRequest(@NotBlank String task) {
}
