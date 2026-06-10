package com.ai.domain.model;

import com.ai.domain.vo.ChatSessionId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChatSession {

    private final ChatSessionId id;
    private final String title;
    private final List<ChatMessage> messages;
    private final Instant createdAt;
    private Instant lastActivityAt;

    ChatSession(ChatSessionId id, String title, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "ChatSessionId cannot be null");
        this.title = validateTitle(title);
        this.messages = new ArrayList<>();
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.lastActivityAt = createdAt;
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            return "New Chat";
        }
        if (title.length() > 100) {
            return title.substring(0, 100);
        }
        return title.trim();
    }

    public static ChatSession create(String title) {
        return new ChatSession(ChatSessionId.generate(), title, Instant.now());
    }

    public static ChatSession of(ChatSessionId id, String title, Instant createdAt) {
        return new ChatSession(id, title, createdAt);
    }

    public ChatSessionId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    /**
     * Adds a user message to the session and updates last activity timestamp.
     */
    public ChatMessage addUserMessage(String text) {
        ChatMessage message = ChatMessage.createUserMessage(text);
        messages.add(message);
        updateLastActivity();
        return message;
    }

    /**
     * Adds an assistant message to the session and updates last activity timestamp.
     */
    public ChatMessage addAssistantMessage(String text) {
        ChatMessage message = ChatMessage.createAssistantMessage(text);
        messages.add(message);
        updateLastActivity();
        return message;
    }

    /**
     * Returns an unmodifiable view of all messages.
     */
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns the number of messages.
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Returns the number of user messages.
     */
    public int getUserMessageCount() {
        return (int) messages.stream()
            .filter(ChatMessage::isFromUser)
            .count();
    }

    /**
     * Returns the number of assistant messages.
     */
    public int getAssistantMessageCount() {
        return (int) messages.stream()
            .filter(ChatMessage::isFromAssistant)
            .count();
    }

    /**
     * Returns the most recent user message.
     */
    public ChatMessage getLastUserMessage() {
        List<ChatMessage> userMessages = messages.stream()
            .filter(ChatMessage::isFromUser)
            .toList();
        return userMessages.isEmpty() ? null : userMessages.get(userMessages.size() - 1);
    }

    /**
     * Returns the most recent assistant message.
     */
    public ChatMessage getLastAssistantMessage() {
        List<ChatMessage> assistantMessages = messages.stream()
            .filter(ChatMessage::isFromAssistant)
            .toList();
        return assistantMessages.isEmpty() ? null : assistantMessages.get(assistantMessages.size() - 1);
    }

    /**
     * Returns the most recent N messages.
     */
    public List<ChatMessage> getRecentMessages(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        int size = messages.size();
        int start = Math.max(0, size - count);
        return Collections.unmodifiableList(messages.subList(start, size));
    }

    /**
     * Checks if the session has no messages.
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /**
     * Clears all messages from the session.
     */
    public void clearMessages() {
        messages.clear();
        updateLastActivity();
    }

    private void updateLastActivity() {
        this.lastActivityAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatSession that = (ChatSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChatSession{id=%s, title='%s', messageCount=%d}".formatted(id, title, messages.size());
    }
}
