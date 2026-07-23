package com.ai.rag.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * RAG chat request DTO.
 */
public record RagChatRequest(
    @JsonProperty("query")
    @JsonAlias("question")
    String question,

    @JsonAlias("session_id")
    String sessionId,

    @JsonAlias("top_k")
    Integer topK,

    Double temperature,

    @JsonAlias("doc_ids")
    List<String> docIds,

    List<String> images
) {
    public RagChatRequest {
        if (topK == null) topK = 5;
        if (temperature == null) temperature = 0.7;
        if (images == null) images = List.of();
    }
}
