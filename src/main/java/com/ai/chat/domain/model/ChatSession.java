package com.ai.chat.domain.model;

import com.ai.base.domain.model.AbstractEntity;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.domain.vo.OwnerKeyAttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@Table(name = "chat_sessions")
@AttributeOverride(
    name = "updatedAt",
    column = @Column(name = "last_activity_at", nullable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class ChatSession extends AbstractEntity<ChatSessionId> {

  public static final String DEFAULT_TITLE = "New Chat";
  static final String ORPHAN_CLIENT_ID = "__orphan__";

  @Convert(converter = OwnerKeyAttributeConverter.class)
  @Column(name = "owner_key", length = 80)
  private OwnerKey ownerKey;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Transient private List<ChatMessage> messages = new ArrayList<>();

  private ChatSession(
      ChatSessionId id, String title, Instant createdAt, OwnerKey ownerKey, boolean orphan) {
    super(id, createdAt, createdAt);
    this.title = validateTitle(title);
    this.ownerKey = orphan ? null : requireOwnerKey(ownerKey);
  }

  private ChatSession(
      ChatSessionId id,
      String title,
      Instant createdAt,
      Instant lastActivityAt,
      OwnerKey ownerKey,
      boolean orphan) {
    super(id, createdAt, lastActivityAt != null ? lastActivityAt : createdAt);
    this.title = validateTitle(title);
    this.ownerKey = orphan ? null : requireOwnerKey(ownerKey);
  }

  private static OwnerKey requireOwnerKey(OwnerKey ownerKey) {
    if (ownerKey == null) {
      throw new IllegalArgumentException("ClientId cannot be null or blank");
    }
    return ownerKey;
  }

  private static OwnerKey parseOwnerKey(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("ClientId cannot be null or blank");
    }
    String trimmed = clientId.trim();
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

  /** Documentation. */
  public static ChatSession create(String title, String clientId) {
    return new ChatSession(
        ChatSessionId.generate(), title, Instant.now(), parseOwnerKey(clientId), false);
  }

  /** Documentation. */
  public static ChatSession createWithId(ChatSessionId id, String title, String clientId) {
    return new ChatSession(id, title, Instant.now(), parseOwnerKey(clientId), false);
  }

  /** Documentation. */
  public static ChatSession of(ChatSessionId id, String title, Instant createdAt, String clientId) {
    return new ChatSession(id, title, createdAt, parseOwnerKey(clientId), false);
  }

  /** Documentation. */
  public static ChatSession reconstitute(
      ChatSessionId id,
      String title,
      Instant createdAt,
      Instant lastActivityAt,
      List<ChatMessage> messages,
      String clientId) {
    ChatSession session =
        new ChatSession(id, title, createdAt, lastActivityAt, parseOwnerKey(clientId), false);
    if (messages != null) {
      session.messages.addAll(messages);
    }
    return session;
  }

  /** Reconstitute a legacy row that has no client ownership (invisible to clients). */
  public static ChatSession reconstituteOrphan(
      ChatSessionId id,
      String title,
      Instant createdAt,
      Instant lastActivityAt,
      List<ChatMessage> messages) {
    ChatSession session = new ChatSession(id, title, createdAt, lastActivityAt, null, true);
    if (messages != null) {
      session.messages.addAll(messages);
    }
    return session;
  }

  /** Returns the persisted owner_key value (c:… or u:…) or {@link #ORPHAN_CLIENT_ID}. */
  public String getClientId() {
    return ownerKey == null ? ORPHAN_CLIENT_ID : ownerKey.value();
  }

  /** Documentation. */
  public Instant getLastActivityAt() {
    return getUpdatedAt();
  }

  /** Documentation. */
  public boolean belongsTo(String otherClientId) {
    return getClientId().equals(otherClientId);
  }

  /** Documentation. */
  public boolean hasDefaultTitle() {
    return DEFAULT_TITLE.equals(title);
  }

  /** Documentation. */
  public void rename(String newTitle) {
    if (newTitle == null || newTitle.isBlank()) {
      return;
    }
    this.title = validateTitle(newTitle);
    updateLastActivity();
  }

  /** Documentation. */
  public ChatMessage addUserMessage(String text) {
    ChatMessage message = ChatMessage.createUserMessage(text);
    messages.add(message);
    updateLastActivity();
    return message;
  }

  /** Documentation. */
  public ChatMessage addAssistantMessage(String text) {
    ChatMessage message = ChatMessage.createAssistantMessage(text);
    messages.add(message);
    updateLastActivity();
    return message;
  }

  /** Documentation. */
  public List<ChatMessage> getMessages() {
    return Collections.unmodifiableList(messages);
  }

  /** Documentation. */
  public int getMessageCount() {
    return messages.size();
  }

  /** Documentation. */
  public int getUserMessageCount() {
    return (int) messages.stream().filter(ChatMessage::isFromUser).count();
  }

  /** Documentation. */
  public int getAssistantMessageCount() {
    return (int) messages.stream().filter(ChatMessage::isFromAssistant).count();
  }

  /** Documentation. */
  public ChatMessage getLastUserMessage() {
    return getLastMessageByRole(ChatMessage::isFromUser);
  }

  /** Documentation. */
  public ChatMessage getLastAssistantMessage() {
    return getLastMessageByRole(ChatMessage::isFromAssistant);
  }

  private ChatMessage getLastMessageByRole(Predicate<ChatMessage> filter) {
    return messages.stream().filter(filter).reduce((first, second) -> second).orElse(null);
  }

  /** Documentation. */
  public List<ChatMessage> getRecentMessages(int count) {
    if (count <= 0) {
      return Collections.emptyList();
    }
    int size = messages.size();
    int start = Math.max(0, size - count);
    return Collections.unmodifiableList(messages.subList(start, size));
  }

  /** Documentation. */
  public boolean isEmpty() {
    return messages.isEmpty();
  }

  /** Documentation. */
  public void clearMessages() {
    messages.clear();
    updateLastActivity();
  }

  /** Documentation. */
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
