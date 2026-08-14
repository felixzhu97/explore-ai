package com.ai.rag.infrastructure.vector;

import com.ai.common.util.LogSanitizer;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.TextEmbeddingRepository;
import com.ai.rag.domain.util.VectorSimilarity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
public class H2SpringAiVectorStore implements VectorStore {

  public static final String DOCUMENT_ID_METADATA_KEY = "document_id";
  private static final Logger log = LoggerFactory.getLogger(H2SpringAiVectorStore.class);
  private static final int MAX_CONTENT_LENGTH = 500;

  private final TextEmbeddingRepository embeddingRepository;
  private final DocumentChunkSearchRepository chunkSearchRepository;

  /** Documentation. */
  public H2SpringAiVectorStore(
      TextEmbeddingRepository embeddingRepository,
      DocumentChunkSearchRepository chunkSearchRepository) {
    this.embeddingRepository = embeddingRepository;
    this.chunkSearchRepository = chunkSearchRepository;
  }

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
    log.info("RAG retrieval for query: {}", LogSanitizer.truncate(query));

    float[] queryEmbedding = embeddingRepository.embed(query);
    int topK = Math.max(request.getTopK(), 1);
    List<UUID> docIds = extractDocumentIds(request.getFilterExpression());

    List<DocumentChunk> chunks =
        docIds == null
            ? chunkSearchRepository.search(queryEmbedding, topK)
            : chunkSearchRepository.search(queryEmbedding, topK, docIds);
    double threshold = request.getSimilarityThreshold();

    List<Document> results = new ArrayList<>();
    for (DocumentChunk chunk : chunks) {
      double score = VectorSimilarity.cosineSimilarity(queryEmbedding, chunk.getEmbedding());
      if (score < threshold) {
        continue;
      }
      results.add(toDocument(chunk, score));
    }
    log.info("Retrieved {} chunks after score threshold {}", results.size(), threshold);
    return results;
  }

  private static Document toDocument(DocumentChunk chunk, double score) {
    Map<String, Object> metadata = new HashMap<>();
    if (chunk.getMetadata() != null) {
      metadata.putAll(chunk.getMetadata());
    }
    metadata.put(DOCUMENT_ID_METADATA_KEY, chunk.getDocumentId().toString());
    metadata.put("score", score);
    return Document.builder()
        .id(chunk.getId().toString())
        .text(LogSanitizer.truncate(chunk.getContent(), MAX_CONTENT_LENGTH))
        .metadata(metadata)
        .score(score)
        .build();
  }

  static List<UUID> extractDocumentIds(Filter.Expression expression) {
    if (expression == null) {
      return null;
    }
    return findDocumentIdIn(expression).orElse(null);
  }

  private static Optional<List<UUID>> findDocumentIdIn(Filter.Expression expression) {
    if (expression.type() == Filter.ExpressionType.IN
        && expression.left() instanceof Filter.Key key
        && DOCUMENT_ID_METADATA_KEY.equals(key.key())
        && expression.right() instanceof Filter.Value value) {
      return Optional.ofNullable(toUuids(value.value()));
    }
    if (expression.type() == Filter.ExpressionType.AND) {
      if (expression.left() instanceof Filter.Expression left) {
        Optional<List<UUID>> fromLeft = findDocumentIdIn(left);
        if (fromLeft.isPresent()) {
          return fromLeft;
        }
      }
      if (expression.right() instanceof Filter.Expression right) {
        return findDocumentIdIn(right);
      }
    }
    return Optional.empty();
  }

  private static List<UUID> toUuids(Object raw) {
    List<UUID> ids = new ArrayList<>();
    if (raw instanceof List<?> list) {
      for (Object item : list) {
        if (item != null) {
          ids.add(UUID.fromString(item.toString()));
        }
      }
    } else if (raw != null) {
      ids.add(UUID.fromString(raw.toString()));
    }
    return ids.isEmpty() ? null : ids;
  }
}
