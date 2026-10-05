package com.ai.rag.infra.etl;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.DocumentChunkRepository;
import com.ai.rag.domain.repository.DocumentWriter;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Infrastructure adapter for embedding and writing document chunks. */
@Component
@RequiredArgsConstructor
public class EmbeddingDocumentWriter implements DocumentWriter {

  private final TextEmbeddingGateway embeddingRepository;
  private final DocumentChunkRepository chunkRepository;

  @Override
  public void write(List<DocumentChunk> chunks) {
    for (DocumentChunk chunk : chunks) {
      float[] embedding = embeddingRepository.embed(chunk.getContent());
      chunkRepository.saveChunk(chunk.withEmbedding(embedding));
    }
  }
}
