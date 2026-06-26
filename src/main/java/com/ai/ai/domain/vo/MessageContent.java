package com.ai.ai.domain.vo;

import java.util.Objects;

/**
 * Message content value object - encapsulates message text and metadata.
 * Immutable, provides content validation.
 */
public final class MessageContent {

    private static final int MAX_LENGTH = 10000;

    private final String text;
    private final MessageRole role;

    public MessageContent(String text) {
        this(text, MessageRole.USER);
    }

    public MessageContent(String text, MessageRole role) {
        this.text = validateText(text);
        this.role = role != null ? role : MessageRole.USER;
    }

    public MessageContent(String text, String role) {
        this(text, MessageRole.from(role));
    }

    private static String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be null or blank");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Message content exceeds maximum length of " + MAX_LENGTH + " characters"
            );
        }
        return text.trim();
    }

    public String text() {
        return text;
    }

    public MessageRole role() {
        return role;
    }

    public String roleValue() {
        return role.value();
    }

    public boolean isFromUser() {
        return MessageRole.USER.equals(role);
    }

    public boolean isFromAssistant() {
        return MessageRole.ASSISTANT.equals(role);
    }

    public MessageContent withRole(MessageRole newRole) {
        return new MessageContent(this.text, newRole);
    }

    public MessageContent withRole(String newRole) {
        return new MessageContent(this.text, MessageRole.from(newRole));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageContent that = (MessageContent) o;
        return Objects.equals(text, that.text) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, role);
    }

    @Override
    public String toString() {
        return "MessageContent{role='%s', text='%s...'}".formatted(role.value(), 
            text.length() > 50 ? text.substring(0, 50) : text);
    }
}
