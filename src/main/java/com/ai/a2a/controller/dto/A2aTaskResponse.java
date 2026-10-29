package com.ai.a2a.controller.dto;

import com.ai.a2a.domain.model.A2aTask;
import java.util.List;
import java.util.Map;

/** HTTP JSON Task shape for A2A message APIs. */
public record A2aTaskResponse(String id, Status status, List<Artifact> artifacts) {

  /** Builds a Task response from the domain Task. */
  public static A2aTaskResponse from(A2aTask task) {
    Status status = new Status(task.getStatus().getWireValue(), statusMessage(task));
    List<Artifact> artifacts =
        task.getArtifacts().stream()
            .map(
                artifact ->
                    new Artifact(
                        stringValue(artifact.get("artifactId")),
                        stringValue(artifact.get("name")),
                        partsFrom(artifact.get("parts"))))
            .toList();
    return new A2aTaskResponse(task.getId(), status, artifacts);
  }

  /** Rebuilds a Task response from the service stream wire map. */
  @SuppressWarnings("unchecked")
  public static A2aTaskResponse fromWireMap(Map<String, Object> wire) {
    if (wire == null) {
      return new A2aTaskResponse("", new Status("unknown", null), List.of());
    }
    String id = stringValue(wire.get("id"));
    Map<String, Object> statusMap =
        wire.get("status") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    Message message = null;
    if (statusMap.get("message") instanceof Map<?, ?> messageMap) {
      message =
          new Message(stringValue(messageMap.get("role")), partsFrom(messageMap.get("parts")));
    }
    Status status = new Status(stringValue(statusMap.get("state")), message);
    List<Artifact> artifacts = List.of();
    if (wire.get("artifacts") instanceof List<?> list) {
      artifacts =
          list.stream()
              .filter(Map.class::isInstance)
              .map(item -> (Map<String, Object>) item)
              .map(
                  artifact ->
                      new Artifact(
                          stringValue(artifact.get("artifactId")),
                          stringValue(artifact.get("name")),
                          partsFrom(artifact.get("parts"))))
              .toList();
    }
    return new A2aTaskResponse(id, status, artifacts);
  }

  private static Message statusMessage(A2aTask task) {
    if (task.getErrorMessage() == null) {
      return null;
    }
    return new Message("agent", List.of(new Part(task.getErrorMessage())));
  }

  @SuppressWarnings("unchecked")
  private static List<Part> partsFrom(Object parts) {
    if (!(parts instanceof List<?> list)) {
      return List.of();
    }
    return list.stream()
        .filter(Map.class::isInstance)
        .map(item -> (Map<String, Object>) item)
        .map(part -> new Part(stringValue(part.get("text"))))
        .toList();
  }

  private static String stringValue(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  /** Task status envelope. */
  public record Status(String state, Message message) {}

  /** Optional agent/user message on the status. */
  public record Message(String role, List<Part> parts) {}

  /** Text part used in status/artifact payloads. */
  public record Part(String text) {}

  /** Completed Task artifact. */
  public record Artifact(String artifactId, String name, List<Part> parts) {}
}
