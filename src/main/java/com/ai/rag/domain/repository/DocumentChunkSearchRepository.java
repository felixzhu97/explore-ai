package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.ScoredChunk;
import java.util.List;
import java.util.UUID;

/** Repository for vector similarity search over persisted document chunks. */
public interface DocumentChunkSearchRepository {
  /**
   * Returns the owner's top-K chunks by cosine similarity, best first, limited to {@code
   * documentIds} when it is not empty.
   */
  List<ScoredChunk> search(
      float[] queryEmbedding, int topK, String ownerKey, List<UUID> documentIds);

  /**
   * Returns up to {@code limit} of the owner's chunks from {@code documentIds}, lowest chunk index
   * first, so each document contributes its opening before any later section.
   */
  List<DocumentChunk> findLeadingChunks(String ownerKey, List<UUID> documentIds, int limit);
}
