package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluatorOptimizerRequest(@NotBlank String task) {}
