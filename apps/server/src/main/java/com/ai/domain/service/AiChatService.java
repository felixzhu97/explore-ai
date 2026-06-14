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

    /**
     * Sends a message to AI and returns a streaming response.
     *
     * @param userMessage user message text
     * @return flux of response text chunks
     */
    reactor.core.publisher.Flux<String> chatStream(String userMessage);

    /**
     * Sends a message with system prompt and returns a streaming response.
     *
     * @param userMessage   user message text
     * @param systemPrompt system prompt to prepend
     * @return flux of response text chunks
     */
    reactor.core.publisher.Flux<String> chatStream(String userMessage, String systemPrompt);
}
