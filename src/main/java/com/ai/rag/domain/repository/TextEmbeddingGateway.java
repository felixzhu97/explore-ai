package com.ai.rag.domain.repository;

import java.util.List;

/** Converts text into embedding vectors, singly or in batches, of a fixed dimension. */
public interface TextEmbeddingGateway {
  float[] embed(String text);

  List<float[]> embedBatch(List<String> texts);

  int getDimensions();
}
