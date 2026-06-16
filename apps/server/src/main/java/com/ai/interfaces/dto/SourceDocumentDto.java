package com.ai.interfaces.dto;

import java.util.Map;

/**
 * Source document DTO for RAG retrieval results.
 */
public record SourceDocumentDto(
    String id,
    String content,
    float score,
    Map<String, Object> metadata,
    int index,
    String documentTitle
) {}
