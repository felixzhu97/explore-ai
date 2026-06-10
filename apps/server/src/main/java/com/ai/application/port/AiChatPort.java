package com.ai.application.port;

import com.ai.domain.model.ChatMessage;

/**
 * AI service port interface - defines application layer requirements for AI services.
 * Dependency inversion: application layer defines interface, infrastructure layer implements.
 */
public interface AiChatPort {

    /**
     * Sends a message to AI and returns the response.
     *
     * @param userMessage user message text
     * @return AI response text
     */
    String chat(String userMessage);

    /**
     * Sends message history to AI and returns the response.
     *
     * @param messages message history
     * @return AI response text
     */
    String chatWithHistory(java.util.List<ChatMessage> messages);
}
