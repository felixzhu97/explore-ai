package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ParallelizationWorkflowRequest(
    @NotBlank @Size(max = WorkflowLimits.MAX_TEXT_LENGTH) String prompt,
    @NotEmpty @Size(max = WorkflowLimits.MAX_ITEMS)
        List<@NotBlank @Size(max = WorkflowLimits.MAX_TEXT_LENGTH) String> items,
    @Min(1) @Max(WorkflowLimits.MAX_PARALLELISM) Integer parallelism) {}
