package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record ChainWorkflowRequest(@NotBlank String userInput, String[] systemPrompts) {}
