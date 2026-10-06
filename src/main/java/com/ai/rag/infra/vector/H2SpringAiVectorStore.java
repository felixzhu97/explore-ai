package com.ai.rag.infra.vector;

import com.ai.common.infra.logging.LogSanitizer;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import com.ai.rag.domain.service.VectorSimilarity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

/**
 * Spring AI {@link VectorStore} backed by existing H2 chunk storage + in-process cosine search.
 * Write path remains domain ETL ({@code EmbeddingDocumentWriter}); this adapter focuses on
 * retrieval.
 */
@Component
@RequiredArgsConstructor
public class H2SpringAiVectorStore implements VectorStore {

  public static final String DOCUMENT_ID_METADATA_KEY = "document_id";
  public static final String OWNER_KEY_METADATA_KEY = "ownerKey";
  private static final Logger log = LoggerFactory.getLogger(H2SpringAiVectorStore.class);
  private static final int MAX_CONTENT_LENGTH = 500;

  private final TextEmbeddingGateway embeddingRepository;
  private final DocumentChunkSearchRepository chunkSearchRepository;

  @Override
  public String getName() {
    return "h2-spring-ai-vector-store";
  }

  @Override
  public void add(List<Document> documents) {
    throw new UnsupportedOperationException(
        "H2SpringAiVectorStore is retrieval-focused; use EmbeddingDocumentWriter for writes");
  }

  @Override
  public void delete(List<String> idList) {
    throw new UnsupportedOperationException(
        "H2SpringAiVectorStore is retrieval-focused; delete via DocumentUploadService");
  }

  @Override
  public void delete(Filter.Expression filterExpression) {
    throw new UnsupportedOperationException(
        "H2SpringAiVectorStore is retrieval-focused; delete via DocumentUploadService");
  }

  @Override
  public List<Document> similaritySearch(SearchRequest request) {
    String query = request.getQuery();
    if (query == null || query.isBlank()) {
      return List.of();
    }
    log.info("RAG retrieval: query length={}", LogSanitizer.lengthOf(query));

    Filter.Expression filter = request.getFilterExpression();
    Optional<String> ownerKey =
        Optional.ofNullable(filter)
            .flatMap(f -> findValue(f, Filter.ExpressionType.EQ, OWNER_KEY_METADATA_KEY))
            .map(Object::toString);
    if (ownerKey.isEmpty()) {
      log.warn("Rejected RAG retrieval without an {} filter", OWNER_KEY_METADATA_KEY);
      return List.of();
    }

    float[] queryEmbedding = embeddingRepository.embed(query);
    int topK = Math.max(request.getTopK(), 1);
    List<UUID> documentIds = extractDocumentIds(filter);
    List<DocumentChunk> chunks =
        chunkSearchRepository.search(queryEmbedding, topK, ownerKey.get(), documentIds);
    double threshold = request.getSimilarityThreshold();

    List<Document> results = new ArrayList<>();
    for (DocumentChunk chunk : chunks) {
      double score =
          VectorSimilarity.calculateCosineSimilarity(queryEmbedding, chunk.getEmbedding());
      if (score < threshold) {
        continue;
      }
      results.add(toDocument(chunk, score));
    }
    log.info("Retrieved {} chunks after score threshold {}", results.size(), threshold);
    if (results.isEmpty() && !documentIds.isEmpty()) {
      return selectLeadingChunks(queryEmbedding, ownerKey.get(), documentIds, topK);
    }
    return results;
  }

  /**
   * Overview questions ("what is this about?") match no passage, so selected documents answer from
   * their opening chunks instead.
   */
  private List<Document> selectLeadingChunks(
      float[] queryEmbedding, String ownerKey, List<UUID> documentIds, int topK) {
    List<Document> results =
        chunkSearchRepository.findLeadingChunks(ownerKey, documentIds, topK).stream()
            .map(
                chunk ->
                    toDocument(
                        chunk,
                        VectorSimilarity.calculateCosineSimilarity(
                            queryEmbedding, chunk.getEmbedding())))
            .toList();
    log.info("Fell back to {} opening chunks of the selected documents", results.size());
    return results;
  }

  private static Document toDocument(DocumentChunk chunk, double score) {
    Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
    metadata.put(DOCUMENT_ID_METADATA_KEY, chunk.getDocumentId().toString());
    metadata.put("score", score);
    return Document.builder()
        .id(chunk.getId().toString())
        .text(LogSanitizer.truncate(chunk.getContent(), MAX_CONTENT_LENGTH))
        .metadata(metadata)
        .score(score)
        .build();
  }

  /** Extracts the document ids from a filter. */
  static List<UUID> extractDocumentIds(Filter.Expression expression) {
    return findValue(expression, Filter.ExpressionType.IN, DOCUMENT_ID_METADATA_KEY)
        .map(H2SpringAiVectorStore::toUuids)
        .orElse(List.of());
  }

  private static Optional<Object> findValue(
      Filter.Expression expression, Filter.ExpressionType type, String metadataKey) {
    if (expression.type() == type
        && expression.left() instanceof Filter.Key key
        && metadataKey.equals(key.key())
        && expression.right() instanceof Filter.Value value) {
      return Optional.ofNullable(value.value());
    }
    if (expression.type() == Filter.ExpressionType.AND) {
      if (expression.left() instanceof Filter.Expression left) {
        Optional<Object> fromLeft = findValue(left, type, metadataKey);
        if (fromLeft.isPresent()) {
          return fromLeft;
        }
      }
      if (expression.right() instanceof Filter.Expression right) {
        return findValue(right, type, metadataKey);
      }
    }
    return Optional.empty();
  }

  private static List<UUID> toUuids(Object raw) {
    List<UUID> ids = new ArrayList<>();
    if (raw instanceof List<?> list) {
      for (Object item : list) {
        ids.add(UUID.fromString(item.toString()));
      }
    } else {
      ids.add(UUID.fromString(raw.toString()));
    }
    return ids;
  }
}
