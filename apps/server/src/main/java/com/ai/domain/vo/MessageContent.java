package com.ai.domain.vo;

import java.util.Objects;

/**
 * Message content value object - encapsulates message text and metadata.
 * Immutable, provides content validation.
 */
public final class MessageContent {

    private static final int MAX_LENGTH = 10000;
    private static final int MIN_LENGTH = 1;

    private final String text;
    private final String role;

    public MessageContent(String text) {
        this(text, "user");
    }

    public MessageContent(String text, String role) {
        this.text = validateText(text);
        this.role = validateRole(role);
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

    private static String validateRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        String normalizedRole = role.toLowerCase().trim();
        if (!normalizedRole.equals("user") && !normalizedRole.equals("assistant")) {
            throw new IllegalArgumentException(
                "Role must be either 'user' or 'assistant', got: " + role
            );
        }
        return normalizedRole;
    }

    public String text() {
        return text;
    }

    public String role() {
        return role;
    }

    public boolean isFromUser() {
        return "user".equals(role);
    }

    public boolean isFromAssistant() {
        return "assistant".equals(role);
    }

    public MessageContent withRole(String newRole) {
        return new MessageContent(this.text, newRole);
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
        return "MessageContent{role='%s', text='%s...'}".formatted(role, 
            text.length() > 50 ? text.substring(0, 50) : text);
    }
}
