package com.ai.chat.domain.model;

import com.ai.common.domain.model.AbstractOwnerAwareEntity;
import com.ai.common.domain.model.OwnerKey;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class ChatSession extends AbstractOwnerAwareEntity<ChatSessionId> {

  @NotNull @Valid @Embedded private SessionTitle title;

  @Transient private List<ChatMessage> messages = new ArrayList<>();

  @NotNull
  @Column(nullable = false)
  private Instant lastActivityAt;

  private ChatSession(ChatSessionId id, SessionTitle title, Instant createdAt, OwnerKey ownerKey) {
    super(id, ownerKey);
    this.title = title;
    this.createdAt = createdAt;
    this.lastActivityAt = createdAt;
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

  /** Returns the message that opened the conversation, if the user has written one. */
  public Optional<ChatMessage> firstUserMessage() {
    return messages.stream().filter(ChatMessage::isFromUser).findFirst();
  }

  /** Returns the newest assistant reply, if there is one. */
  public Optional<ChatMessage> lastAssistantMessage() {
    return messages.stream().filter(ChatMessage::isFromAssistant).reduce((first, last) -> last);
  }

  /** Tells whether the session has no messages. */
  public boolean isEmpty() {
    return messages.isEmpty();
  }

  /** Replaces the transient messages with the stored conversation without recording activity. */
  public void restoreMessages(List<ChatMessage> storedMessages) {
    messages.clear();
    if (storedMessages != null) {
      messages.addAll(storedMessages);
    }
  }

  /** Replaces the transient messages after a new exchange and records the activity. */
  public void recordExchange(List<ChatMessage> storedMessages) {
    restoreMessages(storedMessages);
    updateLastActivity();
  }

  /** Tells whether the session has not been used since {@code cutoff}. */
  public boolean isInactiveSince(Instant cutoff) {
    return getLastActivityAt().isBefore(cutoff);
  }

  private void updateLastActivity() {
    this.lastActivityAt = Instant.now();
  }

  @Override
  public String toString() {
    return "ChatSession{id=%s, title='%s', messageCount=%d}"
        .formatted(getId(), title.value(), messages.size());
  }
}
