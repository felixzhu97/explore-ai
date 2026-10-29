package com.ai.a2a.domain.model;

import com.ai.agent.service.AgentService;
import com.ai.common.exception.DomainException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/** A2A Message with text parts and optional metadata for agent type. */
@Getter
public final class A2aMessage {

  private final String messageId;
  private final String role;
  private final List<Map<String, Object>> parts;
  private final Map<String, Object> metadata;

  private A2aMessage(
      String messageId,
      String role,
      List<Map<String, Object>> parts,
      Map<String, Object> metadata) {
    this.messageId = Objects.requireNonNull(messageId, "messageId");
    this.role = role == null ? "user" : role;
    this.parts = parts == null ? List.of() : List.copyOf(parts);
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  /** Creates a message from protocol fields. */
  public static A2aMessage createMessage(
      String messageId,
      String role,
      List<Map<String, Object>> parts,
      Map<String, Object> metadata) {
    if (messageId == null || messageId.isBlank()) {
      throw DomainException.createInvalidError("A2A_MESSAGE_ID_REQUIRED", "messageId is required");
    }
    return new A2aMessage(messageId, role, parts, metadata);
  }

  /** Joins text parts into the Agent invoke message. */
  public String extractText() {
    StringBuilder text = new StringBuilder();
    for (Map<String, Object> part : parts) {
      Object value = part.get("text");
      if (value instanceof String s && !s.isBlank()) {
        if (!text.isEmpty()) {
          text.append('\n');
        }
        text.append(s);
      }
    }
    if (text.isEmpty()) {
      throw DomainException.createInvalidError(
          "A2A_TEXT_REQUIRED", "At least one text part is required");
    }
    return text.toString();
  }

  /**
   * Resolves catalog Agent Type from metadata {@code agentType} or {@code skill}, else the default.
   */
  public String resolveAgentType(String defaultType) {
    Object typed = metadata.get("agentType");
    if (typed instanceof String s && !s.isBlank()) {
      return s.trim();
    }
    Object skill = metadata.get("skill");
    if (skill instanceof String s && !s.isBlank()) {
      return s.trim();
    }
    return defaultType == null || defaultType.isBlank()
        ? AgentService.DEFAULT_AGENT_TYPE
        : defaultType;
  }
}
