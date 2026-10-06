package com.ai.rag.infra.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.DocumentChunkRepository;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.domain.vo.ScoredChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * H2 vector store adapter. Stores embeddings as JSON arrays and performs cosine-similarity ranking
 * in-process.
 */
@Component
public class H2DocumentChunkRepository
    implements DocumentChunkRepository, DocumentChunkSearchRepository {

  private static final Logger log = LoggerFactory.getLogger(H2DocumentChunkRepository.class);
  private static final String TABLE_NAME = "document_chunks";
  private static final String SELECT_COLUMNS =
      "SELECT id, document_id, owner_key, content, chunk_index, embedding, metadata, created_at";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final ChunkRowMapper chunkRowMapper;

  public H2DocumentChunkRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.chunkRowMapper = new ChunkRowMapper(objectMapper);
  }

  @Override
  @Transactional
  public void saveChunk(DocumentChunk chunk) {
    String embeddingString = toJsonArray(chunk.getEmbedding());
    String metadataJson = serializeMetadata(chunk.getMetadata());
    String sql =
        "MERGE INTO "
            + TABLE_NAME
            + " (id, document_id, content, chunk_index, embedding,"
            + " metadata, created_at, owner_key) "
            + "KEY (id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    jdbcTemplate.update(
        sql,
        chunk.getId().value(),
        chunk.getDocumentId().value(),
        chunk.getContent(),
        chunk.getChunkIndex(),
        embeddingString,
        metadataJson,
        chunk.getCreatedAt().toString(),
        chunk.getOwnerKey().value());
  }

  @Override
  @Transactional(readOnly = true)
  public List<DocumentChunk> findChunksByDocumentId(DocumentId documentId) {
    String sql = SELECT_COLUMNS + " FROM " + TABLE_NAME + " WHERE document_id = ?";
    return jdbcTemplate.query(sql, chunkRowMapper, documentId.value());
  }

  @Override
  @Transactional
  public void deleteChunksByDocumentId(DocumentId documentId) {
    String sql = "DELETE FROM " + TABLE_NAME + " WHERE document_id = ?";
    jdbcTemplate.update(sql, documentId.value());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScoredChunk> search(
      float[] queryEmbedding, int topK, String ownerKey, List<UUID> documentIds) {
    if (queryEmbedding.length == 0) {
      return List.of();
    }
    return loadCandidates(ownerKey, documentIds).stream()
        .filter(chunk -> chunk.isComparableWith(queryEmbedding))
        .map(chunk -> ScoredChunk.of(chunk, queryEmbedding))
        .sorted(ScoredChunk.BEST_FIRST)
        .limit(topK)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DocumentChunk> findLeadingChunks(String ownerKey, List<UUID> documentIds, int limit) {
    if (documentIds.isEmpty()) {
      return List.of();
    }
    StringBuilder sql = buildOwnerScopedSelect(documentIds);
    sql.append(" ORDER BY chunk_index, document_id LIMIT ?");
    List<Object> args = new ArrayList<>();
    args.add(ownerKey);
    args.addAll(documentIds);
    args.add(limit);
    return jdbcTemplate.query(sql.toString(), chunkRowMapper, args.toArray());
  }

  private List<DocumentChunk> loadCandidates(String ownerKey, List<UUID> documentIds) {
    List<Object> args = new ArrayList<>();
    args.add(ownerKey);
    args.addAll(documentIds);
    return jdbcTemplate.query(
        buildOwnerScopedSelect(documentIds).toString(), chunkRowMapper, args.toArray());
  }

  private static StringBuilder buildOwnerScopedSelect(List<UUID> documentIds) {
    StringBuilder sql =
        new StringBuilder(SELECT_COLUMNS)
            .append(" FROM ")
            .append(TABLE_NAME)
            .append(" WHERE owner_key = ?");
    if (!documentIds.isEmpty()) {
      sql.append(" AND document_id IN (")
          .append(documentIds.stream().map(id -> "?").collect(Collectors.joining(",")))
          .append(")");
    }
    return sql;
  }

  private String toJsonArray(float[] array) {
    if (array == null) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < array.length; i++) {
      sb.append(array[i]);
      if (i < array.length - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  private String serializeMetadata(Map<String, Object> metadata) {
    if (metadata.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize metadata", e);
      return null;
    }
  }
}
