package com.ai.rag.domain.model;

import java.util.Map;
import lombok.Value;

/**
 * ExtractedDocument - normalized document view for ETL pipeline. Carries content, metadata, and
 * source identity without framework annotations.
 */
@Value
public class ExtractedDocument {
  String content;
  Map<String, Object> metadata;
  String source;

  public ExtractedDocument(String content, Map<String, Object> metadata, String source) {
    if (content == null) {
      content = "";
    }
    if (metadata == null) {
      metadata = Map.of();
    }
    this.content = content;
    this.metadata = metadata;
    this.source = source;
  }
}
