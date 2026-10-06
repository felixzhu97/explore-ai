package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record RoutingWorkflowRequest(
    @NotBlank @Size(max = WorkflowLimits.MAX_TEXT_LENGTH) String input,
    @NotEmpty @Size(max = WorkflowLimits.MAX_ROUTES) Map<String, String> routes) {}
