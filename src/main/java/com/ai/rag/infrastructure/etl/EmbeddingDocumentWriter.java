package com.ai.rag.infrastructure.etl;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.DocumentWriter;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.repository.TextEmbeddingRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Infrastructure adapter for embedding and writing document chunks.
 */
@Component
public class EmbeddingDocumentWriter implements DocumentWriter {

    private final TextEmbeddingRepository embeddingRepository;
    private final IDocumentChunkRepository chunkRepository;

    public EmbeddingDocumentWriter(
            TextEmbeddingRepository embeddingRepository,
            IDocumentChunkRepository chunkRepository) {
        this.embeddingRepository = embeddingRepository;
        this.chunkRepository = chunkRepository;
    }

    @Override
    public void write(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            float[] embedding = embeddingRepository.embed(chunk.getContent());
            chunkRepository.saveChunk(chunk.withEmbedding(embedding));
        }
    }
}
