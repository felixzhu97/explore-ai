package com.ai.domain.service;

import com.ai.domain.model.ChatMessage;

import java.util.List;

/**
 * AI chat service interface defining AI interaction contract.
 * Implementations are in infrastructure layer using specific AI frameworks.
 */
public interface AiChatService {

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
    String chatWithHistory(List<ChatMessage> messages);
}
