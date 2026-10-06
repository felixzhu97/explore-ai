package com.ai.rag.service;

import com.ai.common.infra.logging.LogSanitizer;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.domain.vo.ScoredChunk;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Document retrieval service - handles vector search and context building. */
@Service
@RequiredArgsConstructor
public class DocumentSearchService {

  private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);

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
    List<ScoredChunk> matches =
        chunkSearchRepository.search(queryEmbedding, effectiveTopK, ownerKey, uuids).stream()
            .filter(scored -> scored.meets(scoreThreshold))
            .sorted(ScoredChunk.BEST_FIRST)
            .toList();

    String context =
        matches.stream()
            .map(scored -> scored.chunk().getContent())
            .collect(Collectors.joining("\n\n"));

    List<SourceDocument> sources =
        matches.stream()
            .map(
                scored ->
                    new SourceDocument(
                        scored.chunk().excerpt(), scored.score(), scored.chunk().getMetadata()))
            .toList();

    log.info("Retrieved {} chunks after score threshold {}", sources.size(), scoreThreshold);
    return new RetrievalResult(context, sources);
  }
}
