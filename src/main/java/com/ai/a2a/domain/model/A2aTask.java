package com.ai.a2a.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/** One A2A Task mapped to a baseline Agent run. */
@Getter
public final class A2aTask {

  private final String id;
  private A2aTaskStatus status;
  private final List<Map<String, Object>> artifacts;
  private String errorMessage;

  private A2aTask(String id) {
    this.id = Objects.requireNonNull(id, "id");
    this.status = A2aTaskStatus.SUBMITTED;
    this.artifacts = new ArrayList<>();
  }

  /** Creates a Task in submitted state. */
  public static A2aTask createSubmitted(String id) {
    return new A2aTask(id);
  }

  /** Moves the Task to working. */
  public void markWorking() {
    this.status = A2aTaskStatus.WORKING;
  }

  /** Completes the Task with a text artifact. */
  public void completeWithText(String text) {
    this.status = A2aTaskStatus.COMPLETED;
    Map<String, Object> artifact = new LinkedHashMap<>();
    artifact.put("artifactId", id + "-reply");
    artifact.put("name", "reply");
    artifact.put("parts", List.of(Map.of("text", text == null ? "" : text)));
    this.artifacts.clear();
    this.artifacts.add(artifact);
  }

  /** Marks the Task failed. */
  public void failWithMessage(String message) {
    this.status = A2aTaskStatus.FAILED;
    this.errorMessage = message == null ? "failed" : message;
  }

  /** Wire shape for HTTP+JSON Task responses. */
  public Map<String, Object> toWireMap() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("state", status.getWireValue());
    if (errorMessage != null) {
      state.put("message", Map.of("role", "agent", "parts", List.of(Map.of("text", errorMessage))));
    }
    Map<String, Object> wire = new LinkedHashMap<>();
    wire.put("id", id);
    wire.put("status", state);
    if (!artifacts.isEmpty()) {
      wire.put("artifacts", List.copyOf(artifacts));
    }
    return wire;
  }
}
