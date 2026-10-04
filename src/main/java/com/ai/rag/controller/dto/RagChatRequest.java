package com.ai.rag.controller.dto;

import java.util.List;

/** RAG chat request DTO. */
public record RagChatRequest(
    String question,
    String sessionId,
    Integer topK,
    Double temperature,
    List<String> documentIds,
    List<String> images) {
  public RagChatRequest {
    if (topK == null) {
      topK = 5;
    }
    if (temperature == null) {
      temperature = 0.7;
    }
    if (images == null) {
      images = List.of();
    }
  }
}
