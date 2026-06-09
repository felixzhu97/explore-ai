package com.ai.text.presentation.dto;

/**
 * Represents a single chat message with role and content.
 */
public record ChatMessage(
        String role,
        String content
) {
    public ChatMessage {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
        if (content == null) {
            content = "";
        }
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public boolean isUser() {
        return "user".equalsIgnoreCase(role);
    }

    public boolean isAssistant() {
        return "assistant".equalsIgnoreCase(role);
    }

    public boolean isSystem() {
        return "system".equalsIgnoreCase(role);
    }
}
