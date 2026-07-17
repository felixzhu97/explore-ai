package com.ai.agent.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PipelineInvokeRequest(
        @NotBlank String message,
        @NotEmpty @Valid List<PipelineNodeRequest> nodes,
        @NotNull @Valid List<PipelineEdgeRequest> edges) {

    public record PipelineNodeRequest(
            @NotBlank String id,
            @NotBlank String agentType) {
    }

    public record PipelineEdgeRequest(
            @NotBlank String sourceId,
            @NotBlank String targetId) {
    }
}
