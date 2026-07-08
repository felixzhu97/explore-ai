package com.ai.rag.domain.model;

import java.util.Map;

/**
 * RawDocument - normalized document view for ETL pipeline.
 * Carries content, metadata, and source identity without framework annotations.
 */
public record RawDocument(
    String content,
    Map<String, Object> metadata,
    String source
) {
    public RawDocument {
        if (content == null) content = "";
        if (metadata == null) metadata = Map.of();
    }
}
