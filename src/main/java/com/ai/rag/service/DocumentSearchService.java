package com.ai.rag.service;

import com.ai.common.infra.logging.LogSanitizer;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import com.ai.rag.domain.service.VectorSimilarity;
import com.ai.rag.domain.vo.DocumentId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Document retrieval service - handles vector search and context building. */
@Service
@RequiredArgsConstructor
public class DocumentSearchService {

  private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);
  private static final int MAX_CONTENT_LENGTH = 500;

  public record RetrievalResult(String context, List<SourceDocument> sources) {}

  private final TextEmbeddingGateway embeddingRepository;
  private final DocumentChunkSearchRepository chunkSearchRepository;
  private final RagRetrievalSettings retrievalSettings;

  /**
   * Embeds the query and returns the owner's chunks above the score threshold as context and
   * sources.
   */
  public RetrievalResult retrieve(
      String query, List<DocumentId> documentIds, int topK, String ownerKey) {
    log.info("RAG retrieval for query length={}", LogSanitizer.lengthOf(query));
    float[] queryEmbedding = embeddingRepository.embed(query);
    int effectiveTopK = topK > 0 ? topK : retrievalSettings.getTopK();
    double scoreThreshold = retrievalSettings.getScoreThreshold();

    List<UUID> uuids =
        documentIds == null ? List.of() : documentIds.stream().map(DocumentId::uuidValue).toList();
    List<DocumentChunk> chunks =
        chunkSearchRepository.search(queryEmbedding, effectiveTopK, ownerKey, uuids);

    List<DocumentChunk> filteredChunks =
        chunks.stream()
            .filter(
                chunk ->
                    VectorSimilarity.calculateCosineSimilarity(queryEmbedding, chunk.getEmbedding())
                        >= scoreThreshold)
            .sorted(
                Comparator.comparingDouble(
                        (DocumentChunk chunk) ->
                            VectorSimilarity.calculateCosineSimilarity(
                                queryEmbedding, chunk.getEmbedding()))
                    .reversed())
            .toList();

    String context =
        filteredChunks.stream()
            .map(DocumentChunk::getContent)
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("");

    List<SourceDocument> sources =
        filteredChunks.stream()
            .map(
                chunk ->
                    new SourceDocument(
                        LogSanitizer.truncate(chunk.getContent(), MAX_CONTENT_LENGTH),
                        VectorSimilarity.calculateCosineSimilarity(
                            queryEmbedding, chunk.getEmbedding()),
                        chunk.getMetadata()))
            .toList();

    log.info("Retrieved {} chunks after score threshold {}", sources.size(), scoreThreshold);
    return new RetrievalResult(context, sources);
  }
}
