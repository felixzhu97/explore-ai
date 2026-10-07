package com.ai.rag.domain.model;

import lombok.Value;

/** Chunk text cited in an answer, with its similarity score and metadata. */
@Value
public class SourceCitation {
  String content;
  double score;
  java.util.Map<String, Object> metadata;
}
