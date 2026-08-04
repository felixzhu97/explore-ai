package com.ai.workflow.web;

import jakarta.validation.constraints.NotBlank;

public record OrchestratorWorkersRequest(@NotBlank String task) {
}
