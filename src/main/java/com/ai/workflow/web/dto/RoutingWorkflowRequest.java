package com.ai.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record RoutingWorkflowRequest(
        @NotBlank String input,
        @NotEmpty Map<String, String> routes) {
}
