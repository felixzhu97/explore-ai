package com.ai.pipeline.controller.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/** How an agent executes: one model call ({@code single}) or a deep multi-step loop. */
public enum AgentRuntime {
  SINGLE("single"),
  DEEP("deep");

  private final String value;

  AgentRuntime(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  /** Maps the normalized runtime of an agent definition; rejects unknown runtimes. */
  public static AgentRuntime createResponse(String text) {
    return Arrays.stream(values())
        .filter(runtime -> runtime.value.equals(text))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported agent runtime: " + text));
  }
}
