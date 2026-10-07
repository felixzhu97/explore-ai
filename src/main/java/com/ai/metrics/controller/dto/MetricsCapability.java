package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.model.AiCapability;
import com.fasterxml.jackson.annotation.JsonValue;

/** AI capability on the metrics wire, serialized as its lowercase value. */
public enum MetricsCapability {
  CHAT,
  RAG,
  AGENTS,
  TOOLS,
  VISION,
  WORKFLOW;

  @JsonValue
  public String value() {
    return aiCapability().value();
  }

  /** Maps a capability to its API value. */
  public static MetricsCapability from(AiCapability capability) {
    return valueOf(capability.name());
  }

  /** Maps a capability value such as {@code chat}; rejects unknown capabilities. */
  public static MetricsCapability fromValue(String raw) {
    return from(AiCapability.require(raw));
  }

  private AiCapability aiCapability() {
    return AiCapability.valueOf(name());
  }
}
