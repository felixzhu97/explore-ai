package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.model.AiDomain;
import com.fasterxml.jackson.annotation.JsonValue;

/** AI domain on the metrics wire, serialized as its lowercase value. */
public enum MetricsDomain {
  CHAT,
  RAG,
  AGENTS,
  TOOLS,
  VISION,
  WORKFLOW;

  @JsonValue
  public String value() {
    return aiDomain().value();
  }

  /** Maps a domain to its API value. */
  public static MetricsDomain from(AiDomain domain) {
    return valueOf(domain.name());
  }

  /** Maps a domain value such as {@code chat}; rejects unknown domains. */
  public static MetricsDomain fromValue(String raw) {
    return from(AiDomain.require(raw));
  }

  private AiDomain aiDomain() {
    return AiDomain.valueOf(name());
  }
}
