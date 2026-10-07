package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.WebSource;
import java.util.List;
import java.util.Map;

/** Persists web citation payloads keyed by conversation and assistant content hash. */
public interface ChatWebSourcesRepository {
  /**
   * Loads web sources for a conversation.
   *
   * @return map of contentHash → sources for the conversation
   */
  Map<String, List<WebSource>> findByConversationId(String conversationId);

  /** Saves the web sources used for an assistant reply. */
  void save(String conversationId, String assistantContent, String query, List<WebSource> sources);

  /** Deletes all web sources of a conversation. */
  void deleteByConversationId(String conversationId);
}
