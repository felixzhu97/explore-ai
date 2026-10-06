package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ChainWorkflowRequest(
    @NotBlank @Size(max = WorkflowLimits.MAX_TEXT_LENGTH) String userInput,
    @Size(max = WorkflowLimits.MAX_CHAIN_STEPS)
        List<@NotBlank @Size(max = WorkflowLimits.MAX_TEXT_LENGTH) String> systemPrompts) {

  /** Returns the system prompts as an array, or {@code null} when none were sent. */
  public String[] systemPromptArray() {
    return systemPrompts == null ? null : systemPrompts.toArray(String[]::new);
  }
}
