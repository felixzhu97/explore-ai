package com.ai.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChainWorkflowRequest(
        @NotBlank String userInput,
        String[] systemPrompts) {
}
