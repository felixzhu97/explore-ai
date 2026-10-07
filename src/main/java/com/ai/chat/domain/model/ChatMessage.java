package com.ai.chat.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Immutable chat message; who wrote it is a {@link MessageRole}. */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ChatMessage {

  @EqualsAndHashCode.Include private final MessageId id;
  private final String text;
  private final MessageRole role;
  private final Instant timestamp;

  private ChatMessage(MessageId id, String text, MessageRole role, Instant timestamp) {
    this.id = Objects.requireNonNull(id, "MessageId cannot be null");
    this.text = validateText(text);
    this.role = Objects.requireNonNull(role, "role");
    this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
  }

  private static String validateText(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Message text cannot be null or blank");
    }
    return text.trim();
  }

  /** Creates a new user message. */
  public static ChatMessage createUserMessage(String text) {
    return new ChatMessage(MessageId.generateId(), text, MessageRole.USER, Instant.now());
  }

  /** Creates a new assistant message. */
  public static ChatMessage createAssistantMessage(String text) {
    return new ChatMessage(MessageId.generateId(), text, MessageRole.ASSISTANT, Instant.now());
  }

  /** Rebuilds a stored message. */
  public static ChatMessage restoreMessage(
      MessageId id, String text, MessageRole role, Instant timestamp) {
    return new ChatMessage(id, text, role, timestamp);
  }

  /** Tells whether the user sent the message. */
  public boolean isFromUser() {
    return role == MessageRole.USER;
  }

  /** Tells whether the assistant sent the message. */
  public boolean isFromAssistant() {
    return role == MessageRole.ASSISTANT;
  }

  @Override
  public String toString() {
    return "ChatMessage{id=%s, role=%s, timestamp=%s}".formatted(id, role, timestamp);
  }
}
