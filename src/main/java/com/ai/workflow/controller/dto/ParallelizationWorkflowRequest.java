package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Documentation. */
public record ParallelizationWorkflowRequest(
    @NotBlank String prompt, @NotEmpty List<String> items, Integer parallelism) {}
