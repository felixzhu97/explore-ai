package com.ai.rag.domain.repository;

/** Documentation. */
public interface RagRetrievalSettings {
  /** Documentation. */
  int getTopK();

  /** Documentation. */
  double getScoreThreshold();
}
