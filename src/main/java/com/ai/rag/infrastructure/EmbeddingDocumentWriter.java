package com.ai.rag.infrastructure;

import com.ai.rag.domain.DocumentChunk;
import com.ai.rag.domain.DocumentWriter;
import com.ai.rag.domain.IDocumentChunkRepository;
import com.ai.rag.domain.TextEmbeddingRepository;
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
