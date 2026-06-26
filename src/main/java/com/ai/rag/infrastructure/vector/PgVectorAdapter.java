package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.vo.DocumentId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * PgVector adapter for vector search.
 * Implements IDocumentChunkRepository - single persistence path for chunks.
 */
@Component
public class PgVectorAdapter implements IDocumentChunkRepository {

    private static final Logger log = LoggerFactory.getLogger(PgVectorAdapter.class);
    private static final String TABLE_NAME = "document_chunks";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ChunkRowMapper chunkRowMapper;

    public PgVectorAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.chunkRowMapper = new ChunkRowMapper(objectMapper);
    }

    // --- IDocumentChunkRepository implementation ---

    @Override
    @Transactional
    public void saveChunk(DocumentChunk chunk) {
        String embeddingString = arrayToPostgresString(chunk.getEmbedding());
        String metadataJson = serializeMetadata(chunk.getMetadata());

        String sql = "INSERT INTO " + TABLE_NAME +
                " (id, document_id, content, chunk_index, embedding, metadata, created_at) " +
                "VALUES (?, ?, ?, ?, ?::vector, ?::jsonb, ?) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "content = EXCLUDED.content, " +
                "embedding = EXCLUDED.embedding, " +
                "metadata = EXCLUDED.metadata";

        jdbcTemplate.update(sql,
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getContent(),
                chunk.getChunkIndex(),
                embeddingString,
                metadataJson,
                chunk.getCreatedAt().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> findChunksByDocumentId(DocumentId documentId) {
        String sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                "FROM " + TABLE_NAME + " WHERE document_id = ?";
        return jdbcTemplate.query(sql, chunkRowMapper, documentId.value());
    }

    @Override
    @Transactional
    public void deleteChunksByDocumentId(DocumentId documentId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE document_id = ?";
        jdbcTemplate.update(sql, documentId.value());
    }

    // --- Vector search (PgVectorAdapter specific) ---

    @Transactional(readOnly = true)
    public List<DocumentChunk> search(float[] queryEmbedding, int topK) {
        return search(queryEmbedding, topK, null);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> search(float[] queryEmbedding, int topK, List<UUID> docIds) {
        String embeddingString = arrayToPostgresString(queryEmbedding);

        if (docIds != null && !docIds.isEmpty()) {
            String sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                    "FROM " + TABLE_NAME + " " +
                    "WHERE document_id = ANY(?) " +
                    "ORDER BY embedding <=> ?::vector " +
                    "LIMIT ?";
            return jdbcTemplate.query(sql, chunkRowMapper,
                    docIds.toArray(new UUID[0]), embeddingString, topK);
        } else {
            String sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                    "FROM " + TABLE_NAME + " " +
                    "ORDER BY embedding <=> ?::vector " +
                    "LIMIT ?";
            return jdbcTemplate.query(sql, chunkRowMapper, embeddingString, topK);
        }
    }

    // --- Helpers ---

    private String arrayToPostgresString(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            return null;
        }
    }
}
