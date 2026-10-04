package com.ai.rag.domain.repository;

/** Default top-K and minimum similarity score used when retrieving document chunks. */
public interface RagRetrievalSettings {
  int getTopK();

  double getScoreThreshold();
}
