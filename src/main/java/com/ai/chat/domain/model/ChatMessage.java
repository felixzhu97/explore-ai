package com.ai.chat.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable chat message; who wrote it is a {@link ChatMessageType}. */
public final class ChatMessage {

  private final MessageId id;
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

  /** Returns the message id. */
  public MessageId getId() {
    return id;
  }

  /** Returns the message text. */
  public String getText() {
    return text;
  }

  /** Returns who wrote the message. */
  public ChatMessageType getMessageType() {
    return messageType;
  }

  /** Returns when the message was created. */
  public Instant getTimestamp() {
    return timestamp;
  }

  /** Tells whether the user sent the message. */
  public boolean isFromUser() {
    return messageType == ChatMessageType.USER;
  }

  /** Tells whether the assistant sent the message. */
  public boolean isFromAssistant() {
    return messageType == ChatMessageType.ASSISTANT;
  }

  /** Returns a copy of the message with new text. */
  public ChatMessage withText(String newText) {
    return new ChatMessage(this.id, newText, this.messageType, this.timestamp);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChatMessage that = (ChatMessage) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "ChatMessage{id=%s, type=%s, timestamp=%s}".formatted(id, messageType, timestamp);
  }
}
