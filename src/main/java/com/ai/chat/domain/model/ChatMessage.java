package com.ai.chat.domain.model;

import com.ai.chat.domain.vo.MessageId;
import java.time.Instant;
import java.util.Objects;

/**
 * Chat message entity containing full message information. Immutable object, created through
 * factory methods. Role is represented as a simple String ("user" or "assistant").
 */
public final class ChatMessage {

  private static final String ROLE_USER = "user";
  private static final String ROLE_ASSISTANT = "assistant";

  private final MessageId id;
  private final String text;
  private final String role;
  private final Instant timestamp;

  private ChatMessage(MessageId id, String text, String role, Instant timestamp) {
    this.id = Objects.requireNonNull(id, "MessageId cannot be null");
    this.text = validateText(text);
    this.role = validateRole(role);
    this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
  }

  private static String validateText(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Message text cannot be null or blank");
    }
    return text.trim();
  }

  private static String validateRole(String role) {
    if (role == null || role.isBlank()) {
      return ROLE_USER;
    }
    String normalized = role.toLowerCase().trim();
    if (!ROLE_USER.equals(normalized) && !ROLE_ASSISTANT.equals(normalized)) {
      return ROLE_USER;
    }
    return normalized;
  }

  /** Creates a new user message. */
  public static ChatMessage createUserMessage(String text) {
    return new ChatMessage(MessageId.generate(), text, ROLE_USER, Instant.now());
  }

  /** Creates a new assistant message. */
  public static ChatMessage createAssistantMessage(String text) {
    return new ChatMessage(MessageId.generate(), text, ROLE_ASSISTANT, Instant.now());
  }

  /** Rebuilds a stored message. */
  public static ChatMessage of(MessageId id, String text, String role, Instant timestamp) {
    return new ChatMessage(id, text, role, timestamp);
  }

  /** Returns the message id. */
  public MessageId getId() {
    return id;
  }

  /** Returns the message text. */
  public String getText() {
    return text;
  }

  /** Returns the sender role. */
  public String role() {
    return role;
  }

  /** Returns when the message was created. */
  public Instant getTimestamp() {
    return timestamp;
  }

  /** Tells whether the user sent the message. */
  public boolean isFromUser() {
    return ROLE_USER.equals(role);
  }

  /** Tells whether the assistant sent the message. */
  public boolean isFromAssistant() {
    return ROLE_ASSISTANT.equals(role);
  }

  /** Returns a copy of the message with new text. */
  public ChatMessage withText(String newText) {
    return new ChatMessage(this.id, newText, this.role, this.timestamp);
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
    return "ChatMessage{id=%s, role='%s', timestamp=%s}".formatted(id, role, timestamp);
  }
}
