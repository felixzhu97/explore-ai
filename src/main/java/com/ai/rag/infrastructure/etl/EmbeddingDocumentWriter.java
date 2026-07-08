package com.ai.rag.infrastructure.etl;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.port.DocumentWriter;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Infrastructure adapter for embedding and writing document chunks.
 */
@Component
public class EmbeddingDocumentWriter implements DocumentWriter {

    private final EmbeddingAdapter embeddingAdapter;
    private final IDocumentChunkRepository chunkRepository;

    public EmbeddingDocumentWriter(EmbeddingAdapter embeddingAdapter, IDocumentChunkRepository chunkRepository) {
        this.embeddingAdapter = embeddingAdapter;
        this.chunkRepository = chunkRepository;
    }

    @Override
    public void write(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            float[] embedding = embeddingAdapter.embed(chunk.getContent());
            chunkRepository.saveChunk(chunk.withEmbedding(embedding));
        }
    }
}
