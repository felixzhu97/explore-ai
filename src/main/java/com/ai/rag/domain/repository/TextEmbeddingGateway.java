package com.ai.rag.domain.repository;

import java.util.List;

/** Converts text into embedding vectors, singly or in batches, of a fixed dimension. */
public interface TextEmbeddingGateway {
  /** Embeds one text. */
  float[] embed(String text);

  /** Embeds many texts. */
  List<float[]> embedBatch(List<String> texts);

  /** Returns the embedding size. */
  int getDimensions();
}
