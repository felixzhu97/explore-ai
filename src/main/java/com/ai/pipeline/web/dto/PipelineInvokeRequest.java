package com.ai.pipeline.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request body for invoking a pipeline with a user message and graph of nodes/edges.
 *
 * @param message user message
 * @param nodes pipeline nodes
 * @param edges pipeline edges
 */
public record PipelineInvokeRequest(
    @NotBlank String message,
    @NotEmpty @Valid List<PipelineNodeRequest> nodes,
    @NotNull @Valid List<PipelineEdgeRequest> edges) {

  /** A single agent node in the pipeline graph. */
  public record PipelineNodeRequest(
      @NotBlank String id,
      @NotBlank String agentType,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {}

  /** A directed edge between two pipeline nodes. */
  public record PipelineEdgeRequest(@NotBlank String sourceId, @NotBlank String targetId) {}
}
