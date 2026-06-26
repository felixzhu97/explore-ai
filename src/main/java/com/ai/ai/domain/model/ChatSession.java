package com.ai.ai.domain.model;

import com.ai.ai.domain.vo.ChatSessionId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

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

    public static ChatSession reconstitute(ChatSessionId id, String title, Instant createdAt,
                                         Instant lastActivityAt, List<ChatMessage> messages) {
        ChatSession session = new ChatSession(id, title, createdAt);
        session.lastActivityAt = lastActivityAt != null ? lastActivityAt : createdAt;
        session.messages.addAll(messages);
        return session;
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

    public ChatMessage addUserMessage(String text) {
        ChatMessage message = ChatMessage.createUserMessage(text);
        messages.add(message);
        updateLastActivity();
        return message;
    }

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
        return (int) messages.stream()
            .filter(ChatMessage::isFromUser)
            .count();
    }

    public int getAssistantMessageCount() {
        return (int) messages.stream()
            .filter(ChatMessage::isFromAssistant)
            .count();
    }

    public ChatMessage getLastUserMessage() {
        return getLastMessageByRole(ChatMessage::isFromUser);
    }

    public ChatMessage getLastAssistantMessage() {
        return getLastMessageByRole(ChatMessage::isFromAssistant);
    }

    private ChatMessage getLastMessageByRole(Predicate<ChatMessage> filter) {
        return messages.stream()
            .filter(filter)
            .reduce((first, second) -> second)
            .orElse(null);
    }

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
