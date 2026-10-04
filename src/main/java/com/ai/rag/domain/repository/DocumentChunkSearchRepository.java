package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.DocumentChunk;
import java.util.List;
import java.util.UUID;

/** Repository for vector similarity search over persisted document chunks. */
public interface DocumentChunkSearchRepository {
  /**
   * Returns the owner's top-K chunks by cosine similarity, limited to {@code documentIds} when it
   * is not empty.
   */
  List<DocumentChunk> search(
      float[] queryEmbedding, int topK, String ownerKey, List<UUID> documentIds);
}
