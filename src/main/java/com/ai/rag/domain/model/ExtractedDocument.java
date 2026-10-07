package com.ai.rag.domain.model;

import java.util.Map;

/**
 * ExtractedDocument - normalized document view for ETL pipeline. Carries content, metadata, and
 * source identity without framework annotations.
 */
public record ExtractedDocument(String content, Map<String, Object> metadata, String source) {
  public ExtractedDocument {
    if (content == null) {
      content = "";
    }
    if (metadata == null) {
      metadata = Map.of();
    }
  }
}
