package com.ai.chat.service;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ContentHash;
import com.ai.chat.domain.model.WebSource;
import java.util.List;
import java.util.Map;

/** Messages of one owned session with the web sources cited by its assistant replies. */
public record SessionHistory(
    List<ChatMessage> messages, Map<String, List<WebSource>> sourcesByContentHash) {

  public SessionHistory {
    messages = List.copyOf(messages);
    sourcesByContentHash = Map.copyOf(sourcesByContentHash);
  }

  /** Returns the web sources an assistant reply cited, or an empty list. */
  public List<WebSource> findSources(ChatMessage message) {
    if (!message.isFromAssistant() || sourcesByContentHash.isEmpty()) {
      return List.of();
    }
    return sourcesByContentHash.getOrDefault(
        ContentHash.computeSha256(message.getText()), List.of());
  }

  @Override
  public String toString() {
    return "SessionHistory{messages=%d, citedReplies=%d}"
        .formatted(messages.size(), sourcesByContentHash.size());
  }
}
