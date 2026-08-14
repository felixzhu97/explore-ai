package com.ai.chat.domain.repository;

import com.ai.chat.domain.vo.WebSource;
import java.util.List;
import java.util.Map;

/** Persists web citation payloads keyed by conversation and assistant content hash. */
public interface ChatWebSourcesRepository {
  /** Documentation. */
  void save(String conversationId, String assistantContent, String query, List<WebSource> sources);

  /**
   * Loads web sources for a conversation.
   *
   * @return map of contentHash → sources for the conversation
   */
  Map<String, List<WebSource>> findByConversationId(String conversationId);

  /** Documentation. */
  void deleteByConversationId(String conversationId);
}
