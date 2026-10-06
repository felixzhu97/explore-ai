package com.ai.rag.domain.vo;

import com.ai.rag.domain.model.DocumentChunk;
import java.util.Comparator;
import java.util.Objects;

/** A chunk with its similarity to one query, computed once. */
public record ScoredChunk(DocumentChunk chunk, double score) {

  /** Orders the most similar chunk first. */
  public static final Comparator<ScoredChunk> BEST_FIRST =
      Comparator.comparingDouble(ScoredChunk::score).reversed();

  public ScoredChunk {
    Objects.requireNonNull(chunk, "chunk");
  }

  /** Scores the chunk against the query embedding. */
  public static ScoredChunk of(DocumentChunk chunk, float[] queryEmbedding) {
    return new ScoredChunk(chunk, chunk.similarityTo(queryEmbedding));
  }

  /** Tells whether the score reaches the threshold. */
  public boolean meets(double threshold) {
    return score >= threshold;
  }
}
