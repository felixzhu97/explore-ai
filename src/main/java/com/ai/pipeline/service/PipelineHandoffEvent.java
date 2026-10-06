package com.ai.pipeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SSE {@code agent_handoff} data announcing which agent takes over and why.
 *
 * @param reason routing reason, empty when the router gave none
 */
public record PipelineHandoffEvent(String agentType, String reason) {

  private static final ObjectMapper JSON = new ObjectMapper();

  /** Creates a handoff event. */
  public static PipelineHandoffEvent of(String agentType, String reason) {
    return new PipelineHandoffEvent(agentType, reason == null ? "" : reason);
  }

  /** Serializes the event as SSE data. */
  public String toJson() {
    try {
      return JSON.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to encode handoff event", e);
    }
  }
}
