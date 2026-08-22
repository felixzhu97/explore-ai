package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record OrchestratorWorkersRequest(@NotBlank String task) {}
