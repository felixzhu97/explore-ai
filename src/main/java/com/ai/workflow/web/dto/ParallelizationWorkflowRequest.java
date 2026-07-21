package com.ai.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ParallelizationWorkflowRequest(
        @NotBlank String prompt,
        @NotEmpty List<String> items,
        Integer parallelism) {
}
