package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.DocumentChunk;
import java.util.List;
import java.util.UUID;

/** Repository for vector similarity search over persisted document chunks. */
public interface DocumentChunkSearchRepository {
  /** Documentation. */
  List<DocumentChunk> search(float[] queryEmbedding, int topK);

  /** Documentation. */
  List<DocumentChunk> search(float[] queryEmbedding, int topK, List<UUID> documentIds);
}
