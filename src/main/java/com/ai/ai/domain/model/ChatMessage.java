package com.ai.ai.domain.model;

import com.ai.ai.domain.vo.MessageContent;
import com.ai.ai.domain.vo.MessageId;
import com.ai.ai.domain.vo.MessageRole;

import java.time.Instant;
import java.util.Objects;

/**
 * Chat message entity containing full message information.
 * Immutable object, created through factory methods.
 */
public final class ChatMessage {

    private final MessageId id;
    private final MessageContent content;
    private final Instant timestamp;

    private ChatMessage(MessageId id, MessageContent content, Instant timestamp) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
    }

    public static ChatMessage createUserMessage(String text) {
        return new ChatMessage(
            MessageId.generate(),
            new MessageContent(text, MessageRole.USER),
            Instant.now()
        );
    }

    public static ChatMessage createAssistantMessage(String text) {
        return new ChatMessage(
            MessageId.generate(),
            new MessageContent(text, MessageRole.ASSISTANT),
            Instant.now()
        );
    }

    public static ChatMessage of(MessageId id, String text, MessageRole role, Instant timestamp) {
        return new ChatMessage(id, new MessageContent(text, role), timestamp);
    }

    public MessageId getId() {
        return id;
    }

    public String getText() {
        return content.text();
    }

    public MessageRole getRole() {
        return content.role();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isFromUser() {
        return content.isFromUser();
    }

    public boolean isFromAssistant() {
        return content.isFromAssistant();
    }

    public ChatMessage withText(String newText) {
        return new ChatMessage(this.id, new MessageContent(newText, this.content.role()), this.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChatMessage{id=%s, role='%s', timestamp=%s}".formatted(id, content.role().value(), timestamp);
    }
}
