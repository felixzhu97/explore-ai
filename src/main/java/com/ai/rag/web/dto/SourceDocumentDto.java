package com.ai.rag.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Source document DTO for RAG retrieval results.
 */
public record SourceDocumentDto(
    String id,
    @JsonProperty("text") String content,
    float score,
    Map<String, Object> metadata
) {}
