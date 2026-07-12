package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;

import java.util.List;

/**
 * Repository for synchronizing LLM conversation memory with domain chat sessions.
 */
public interface ConversationMemoryRepository {

    void seedIfEmpty(String conversationId, List<ChatMessage> existingMessages);

    void syncToSession(String conversationId, ChatSession session);

    void clear(String conversationId);
}
