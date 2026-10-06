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

  private static String validateTitle(String title) {
    if (title == null || title.isBlank()) {
      return DEFAULT_TITLE;
    }
    if (title.length() > 100) {
      return title.substring(0, 100);
    }
    return title.trim();
  }

  /** Creates a new chat session for the owner. */
  public static ChatSession create(String title, String ownerKey) {
    return new ChatSession(
        ChatSessionId.generate(), title, Instant.now(), OwnerKey.parse(ownerKey));
  }

  /** Creates a new chat session with a given id. */
  public static ChatSession createWithId(ChatSessionId id, String title, String ownerKey) {
    return new ChatSession(id, title, Instant.now(), OwnerKey.parse(ownerKey));
  }

  /** Rebuilds a stored chat session. */
  public static ChatSession of(ChatSessionId id, String title, Instant createdAt, String ownerKey) {
    return new ChatSession(id, title, createdAt, OwnerKey.parse(ownerKey));
  }

  /** Returns when the session was last active. */
  public Instant getLastActivityAt() {
    return getUpdatedAt();
  }

  /** Tells whether the session still has the default title. */
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

  /** Returns the messages as a read-only list. */
  public List<ChatMessage> getMessages() {
    return Collections.unmodifiableList(messages);
  }

  /** Counts all messages. */
  public int getMessageCount() {
    return messages.size();
  }

  /** Counts the user messages. */
  public int getUserMessageCount() {
    return (int) messages.stream().filter(ChatMessage::isFromUser).count();
  }

  /** Counts the assistant messages. */
  public int getAssistantMessageCount() {
    return (int) messages.stream().filter(ChatMessage::isFromAssistant).count();
  }

  /** Returns the newest user message, or null. */
  public ChatMessage getLastUserMessage() {
    return getLastMessageByRole(ChatMessage::isFromUser);
  }

  /** Returns the newest assistant message, or null. */
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

  /** Tells whether the session has no messages. */
  public boolean isEmpty() {
    return messages.isEmpty();
  }

  /** Removes all messages. */
  public void clearMessages() {
    messages.clear();
    updateLastActivity();
  }

  /** Replaces the transient messages with the stored conversation without recording activity. */
  public void restoreMessages(List<ChatMessage> storedMessages) {
    messages.clear();
    if (storedMessages != null) {
      messages.addAll(storedMessages);
    }
  }

  /** Records that the session was just used, for example after a new exchange. */
  public void recordActivity() {
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
