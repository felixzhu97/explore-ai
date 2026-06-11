package com.ai.application.port;

import com.ai.domain.model.DocumentChunk;
import java.util.List;
import java.util.UUID;

public interface VectorSearchPort {
    List<DocumentChunk> search(float[] queryEmbedding, int topK);
    List<DocumentChunk> search(float[] queryEmbedding, int topK, List<UUID> docIds);
    void saveChunk(DocumentChunk chunk);
}
