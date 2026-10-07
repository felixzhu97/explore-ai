package com.ai.rag.service;

import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.domain.model.ScoredChunk;
import com.ai.rag.domain.model.SourceCitation;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Document retrieval service - handles vector search and context building. */
@Service
@RequiredArgsConstructor
public class DocumentSearchService {

  public record RetrievalResult(String context, List<SourceCitation> sources) {}

  private final TextEmbeddingGateway embeddingRepository;
  private final DocumentChunkSearchRepository chunkSearchRepository;
  private final RagRetrievalSettings retrievalSettings;

  /**
   * Embeds the query and returns the owner's chunks above the score threshold as context and
   * sources.
   */
  public RetrievalResult retrieve(
      String query, List<DocumentId> documentIds, int topK, String ownerKey) {
    float[] queryEmbedding = embeddingRepository.embedText(query);
    int effectiveTopK = topK > 0 ? topK : retrievalSettings.getTopK();
    double scoreThreshold = retrievalSettings.getScoreThreshold();

    List<UUID> uuids =
        documentIds == null ? List.of() : documentIds.stream().map(DocumentId::getValue).toList();
    List<ScoredChunk> matches =
        chunkSearchRepository.searchChunks(queryEmbedding, effectiveTopK, ownerKey, uuids).stream()
            .filter(scored -> scored.meetsThreshold(scoreThreshold))
            .sorted(ScoredChunk.BEST_FIRST)
            .toList();

    String context =
        matches.stream()
            .map(scored -> scored.getChunk().getContent())
            .collect(Collectors.joining("\n\n"));

    List<SourceCitation> sources =
        matches.stream()
            .map(
                scored ->
                    new SourceCitation(
                        scored.getChunk().getExcerpt(),
                        scored.getScore(),
                        scored.getChunk().getMetadata()))
            .toList();

    return new RetrievalResult(context, sources);
  }
}
