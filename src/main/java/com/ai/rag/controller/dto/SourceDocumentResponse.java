package com.ai.rag.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Source document DTO for RAG retrieval results. */
public record SourceDocumentResponse(
    String id, @JsonProperty("text") String content, float score, Map<String, Object> metadata) {}
