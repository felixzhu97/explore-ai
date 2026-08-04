package com.ai.rag.domain;

import com.ai.rag.domain.DocumentChunk;

import java.util.List;
import java.util.UUID;

/**
 * Repository for vector similarity search over persisted document chunks.
 */
public interface DocumentChunkSearchRepository {

    List<DocumentChunk> search(float[] queryEmbedding, int topK);

    List<DocumentChunk> search(float[] queryEmbedding, int topK, List<UUID> documentIds);
}
