package com.ai.chat.domain.repository;

import com.ai.chat.domain.vo.WebSource;
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

  void save(String conversationId, String assistantContent, String query, List<WebSource> sources);

  void deleteByConversationId(String conversationId);
}
