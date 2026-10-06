package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import java.util.List;

/** Repository for synchronizing LLM conversation memory with domain chat sessions. */
public interface ConversationMemoryRepository {
  /** Clears the model memory of a conversation. */
  void clear(String conversationId);

  /** Loads existing messages into memory when it is empty. */
  void seedIfEmpty(String conversationId, List<ChatMessage> existingMessages);

  /** Copies the memory into the session's messages. */
  void syncToSession(String conversationId, ChatSession session);
}
