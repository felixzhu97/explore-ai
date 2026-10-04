package com.ai.chat.domain.model;

import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.domain.model.AbstractOwnerKeyedEntity;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chat session aggregate root with JPA mapping on chat_sessions metadata. Messages are transient
 * and synchronized from Spring AI ChatMemory.
 */
@Entity
@AttributeOverride(
    name = "updatedAt",
    column = @Column(name = "last_activity_at", nullable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class ChatSession extends AbstractOwnerKeyedEntity<ChatSessionId> {

  public static final String DEFAULT_TITLE = "New Chat";

  @NotBlank
  @Size(max = 100)
  @Column(nullable = false, length = 100)
  private String title;

  @Transient private List<ChatMessage> messages = new ArrayList<>();

  private ChatSession(ChatSessionId id, String title, Instant createdAt, OwnerKey ownerKey) {
    super(id, ownerKey, createdAt, createdAt);
    this.title = validateTitle(title);
  }

  private static OwnerKey parseOwnerKey(String ownerKey) {
    if (ownerKey == null || ownerKey.isBlank()) {
      throw new IllegalArgumentException("ClientId cannot be null or blank");
    }
    String trimmed = ownerKey.trim();
    if (trimmed.startsWith(OwnerKey.CLIENT_PREFIX) || trimmed.startsWith(OwnerKey.ACCOUNT_PREFIX)) {
      return OwnerKey.parse(trimmed);
    }
    return OwnerKey.forClient(trimmed);
  }

  private static String validateTitle(String title) {
    if (title == null || title.isBlank()) {
      return DEFAULT_TITLE;
    }
    if (title.length() > 100) {
      return title.substring(0, 100);
    }
    return title.trim();
  }

  public static ChatSession create(String title, String ownerKey) {
    return new ChatSession(ChatSessionId.generate(), title, Instant.now(), parseOwnerKey(ownerKey));
  }

  public static ChatSession createWithId(ChatSessionId id, String title, String ownerKey) {
    return new ChatSession(id, title, Instant.now(), parseOwnerKey(ownerKey));
  }

  public static ChatSession of(ChatSessionId id, String title, Instant createdAt, String ownerKey) {
    return new ChatSession(id, title, createdAt, parseOwnerKey(ownerKey));
  }

  public Instant getLastActivityAt() {
    return getUpdatedAt();
  }

  public boolean belongsTo(String otherClientId) {
    return belongsToClient(otherClientId);
  }

  public boolean hasDefaultTitle() {
    return DEFAULT_TITLE.equals(title);
  }

  /** Renames the session, ignoring blank titles and truncating to 100 characters. */
  public void rename(String newTitle) {
    if (newTitle == null || newTitle.isBlank()) {
      return;
    }
    this.title = validateTitle(newTitle);
    updateLastActivity();
  }

  /** Appends a new user message and records activity. */
  public ChatMessage addUserMessage(String text) {
    ChatMessage message = ChatMessage.createUserMessage(text);
    messages.add(message);
    updateLastActivity();
    return message;
  }

  /** Appends a new assistant message and records activity. */
  public ChatMessage addAssistantMessage(String text) {
    ChatMessage message = ChatMessage.createAssistantMessage(text);
    messages.add(message);
    updateLastActivity();
    return message;
  }

  public List<ChatMessage> getMessages() {
    return Collections.unmodifiableList(messages);
  }

  public int getMessageCount() {
    return messages.size();
  }

  public int getUserMessageCount() {
    return (int) messages.stream().filter(ChatMessage::isFromUser).count();
  }

  public int getAssistantMessageCount() {
    return (int) messages.stream().filter(ChatMessage::isFromAssistant).count();
  }

  public ChatMessage getLastUserMessage() {
    return getLastMessageByRole(ChatMessage::isFromUser);
  }

  public ChatMessage getLastAssistantMessage() {
    return getLastMessageByRole(ChatMessage::isFromAssistant);
  }

  private ChatMessage getLastMessageByRole(Predicate<ChatMessage> filter) {
    return messages.stream().filter(filter).reduce((first, second) -> second).orElse(null);
  }

  /** Returns an unmodifiable view of the last {@code count} messages, or empty if non-positive. */
  public List<ChatMessage> getRecentMessages(int count) {
    if (count <= 0) {
      return Collections.emptyList();
    }
    int size = messages.size();
    int start = Math.max(0, size - count);
    return Collections.unmodifiableList(messages.subList(start, size));
  }

  public boolean isEmpty() {
    return messages.isEmpty();
  }

  public void clearMessages() {
    messages.clear();
    updateLastActivity();
  }

  /** Replaces all transient messages with the given list and records activity. */
  public void replaceMessages(List<ChatMessage> newMessages) {
    messages.clear();
    if (newMessages != null) {
      messages.addAll(newMessages);
    }
    updateLastActivity();
  }

  private void updateLastActivity() {
    touchUpdatedAt();
  }

  @Override
  public String toString() {
    return "ChatSession{id=%s, title='%s', messageCount=%d}"
        .formatted(getId(), title, messages.size());
  }
}
