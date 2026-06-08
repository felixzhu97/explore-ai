package com.ai.agents.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Chat message in a conversation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
    /**
     * Message role: "user" or "assistant".
     */
    String role,

    /**
     * Message content.
     */
    String content
) {
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
