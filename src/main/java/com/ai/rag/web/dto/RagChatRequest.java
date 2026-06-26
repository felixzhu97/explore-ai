package com.ai.rag.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * RAG chat request DTO.
 */
public record RagChatRequest(
    @JsonProperty("query")
    String question,

    @JsonProperty("session_id")
    String sessionId,

    @JsonProperty("top_k")
    Integer topK,

    @JsonProperty("temperature")
    Double temperature,

    @JsonProperty("doc_ids")
    List<String> docIds
) {
    public RagChatRequest {
        if (topK == null) topK = 5;
        if (temperature == null) temperature = 0.7;
    }
}
