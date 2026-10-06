package com.ai.chat.domain.model;

import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.chat.domain.vo.SessionTitle;
import com.ai.common.domain.model.AbstractOwnerKeyedEntity;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

  @NotNull @Valid @Embedded private SessionTitle title;

  @Transient private List<ChatMessage> messages = new ArrayList<>();

  private ChatSession(ChatSessionId id, SessionTitle title, Instant createdAt, OwnerKey ownerKey) {
    super(id, ownerKey, createdAt, createdAt);
    this.title = title;
  }

  /** Creates a new chat session for the owner with the title the user typed. */
  public static ChatSession create(String title, String ownerKey) {
    return new ChatSession(
        ChatSessionId.generate(), SessionTitle.of(title), Instant.now(), OwnerKey.parse(ownerKey));
  }

  /** Starts the owner's default session; it gets a generated title after the first exchange. */
  public static ChatSession startDefault(String ownerKey) {
    return startWithId(ChatSessionId.generate(), ownerKey);
  }

  /** Starts an untitled session under an id the client chose. */
  public static ChatSession startWithId(ChatSessionId id, String ownerKey) {
    return new ChatSession(id, SessionTitle.DEFAULT, Instant.now(), OwnerKey.parse(ownerKey));
  }

  /** Rebuilds a stored chat session. */
  public static ChatSession of(ChatSessionId id, String title, Instant createdAt, String ownerKey) {
    return new ChatSession(id, SessionTitle.of(title), createdAt, OwnerKey.parse(ownerKey));
  }

  /** Returns the title text. */
  public String getTitle() {
    return title.value();
  }

  /** Returns when the session was last active. */
  public Instant getLastActivityAt() {
    return getUpdatedAt();
  }

  /** Renames the session as the user asked, ignoring blank titles. */
  public void rename(String newTitle) {
    if (newTitle == null || newTitle.isBlank()) {
      return;
    }
    this.title = SessionTitle.of(newTitle);
    updateLastActivity();
  }

  /** Tells whether the session is still untitled and has an exchange to name it after. */
  public boolean needsGeneratedTitle() {
    return title.isDefault()
        && firstUserMessage().isPresent()
        && lastAssistantMessage().filter(reply -> !reply.getText().isBlank()).isPresent();
  }

  /**
   * Applies a generated title unless the user named the session meanwhile. Naming is not activity,
   * so the last activity time stays as it was.
   *
   * @return whether the title was applied
   */
  public boolean applyGeneratedTitle(SessionTitle generated) {
    if (!title.isDefault() || generated == null || generated.isDefault()) {
      return false;
    }
    this.title = generated;
    return true;
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

  /** Returns the message that opened the conversation, if the user has written one. */
  public Optional<ChatMessage> firstUserMessage() {
    return messages.stream().filter(ChatMessage::isFromUser).findFirst();
  }

  /** Returns the newest assistant reply, if there is one. */
  public Optional<ChatMessage> lastAssistantMessage() {
    return messages.stream().filter(ChatMessage::isFromAssistant).reduce((first, last) -> last);
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
        .formatted(getId(), title.value(), messages.size());
  }
}
