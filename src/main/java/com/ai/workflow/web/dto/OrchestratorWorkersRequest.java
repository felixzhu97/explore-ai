package com.ai.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record OrchestratorWorkersRequest(@NotBlank String task) {}
