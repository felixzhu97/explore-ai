package com.ai.rag.domain.repository;

/** Default top-K and minimum similarity score used when retrieving document chunks. */
public interface RagRetrievalSettings {
  /** Returns how many chunks to retrieve. */
  int getTopK();

  /** Returns the minimum similarity score. */
  double getScoreThreshold();
}
