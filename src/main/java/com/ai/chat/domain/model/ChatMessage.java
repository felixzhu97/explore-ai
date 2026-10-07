package com.ai.chat.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Immutable chat message; who wrote it is a {@link ChatMessageType}. */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ChatMessage {

  @EqualsAndHashCode.Include private final MessageId id;
  private final String text;
  private final ChatMessageType messageType;
  private final Instant timestamp;

  private ChatMessage(MessageId id, String text, ChatMessageType messageType, Instant timestamp) {
    this.id = Objects.requireNonNull(id, "MessageId cannot be null");
    this.text = validateText(text);
    this.messageType = Objects.requireNonNull(messageType, "messageType");
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
    return new ChatMessage(MessageId.generate(), text, ChatMessageType.USER, Instant.now());
  }

  /** Creates a new assistant message. */
  public static ChatMessage createAssistantMessage(String text) {
    return new ChatMessage(MessageId.generate(), text, ChatMessageType.ASSISTANT, Instant.now());
  }

  /** Rebuilds a stored message. */
  public static ChatMessage restore(
      MessageId id, String text, ChatMessageType messageType, Instant timestamp) {
    return new ChatMessage(id, text, messageType, timestamp);
  }

  /** Tells whether the user sent the message. */
  public boolean isFromUser() {
    return messageType == ChatMessageType.USER;
  }

  /** Tells whether the assistant sent the message. */
  public boolean isFromAssistant() {
    return messageType == ChatMessageType.ASSISTANT;
  }

  @Override
  public String toString() {
    return "ChatMessage{id=%s, type=%s, timestamp=%s}".formatted(id, messageType, timestamp);
  }
}
