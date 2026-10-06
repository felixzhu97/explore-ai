package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatMessage;
import java.util.List;

/** Conversation memory the model reads, mirrored as domain chat messages. */
public interface ConversationMemoryRepository {
  /** Clears the model memory of a conversation. */
  void clear(String conversationId);

  /** Loads existing messages into memory when it is empty. */
  void seedIfEmpty(String conversationId, List<ChatMessage> existingMessages);

  /** Loads the user and assistant messages of a conversation in order. */
  List<ChatMessage> load(String conversationId);
}
