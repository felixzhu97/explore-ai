package com.ai.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;

public record OrchestratorWorkersRequest(@NotBlank String task) {
}
