package com.ai.rag.domain.repository;

import java.util.List;

/** Documentation. */
public interface TextEmbeddingRepository {
  /** Documentation. */
  float[] embed(String text);

  /** Documentation. */
  List<float[]> embedBatch(List<String> texts);

  /** Documentation. */
  int getDimensions();
}
