package com.ai.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/** Documentation. */
public record RoutingWorkflowRequest(
    @NotBlank String input, @NotEmpty Map<String, String> routes) {}
