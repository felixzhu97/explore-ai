package com.ai.infrastructure.adapter.vector;

import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.DocumentChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PgVector adapter implementing VectorSearchPort.
 * Uses JDBC for raw SQL operations against PostgreSQL with pgvector extension.
 */
@Component
public class PgVectorAdapter implements VectorSearchPort {

    private static final Logger log = LoggerFactory.getLogger(PgVectorAdapter.class);

    private static final String TABLE_NAME = "document_chunks";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PgVectorAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<DocumentChunk> search(float[] queryEmbedding, int topK) {
        log.debug("Searching vectors with topK={}", topK);
        return search(queryEmbedding, topK, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> search(float[] queryEmbedding, int topK, List<UUID> docIds) {
        log.debug("Searching vectors with topK={}, docIds filter={}", topK, 
                  docIds != null ? docIds.size() : "none");
        
        String embeddingString = arrayToPostgresString(queryEmbedding);
        
        String sql;
        if (docIds != null && !docIds.isEmpty()) {
            // Use parameterized query with = ANY(?) for SQL injection prevention
            sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                  "FROM " + TABLE_NAME + " " +
                  "WHERE document_id = ANY(?) " +
                  "ORDER BY embedding <=> ?::vector " +
                  "LIMIT ?";

            log.debug("Executing vector search SQL with docIds filter");
            return jdbcTemplate.query(sql,
                    new Object[]{docIds.toArray(new UUID[0]), embeddingString, topK},
                    new ChunkRowMapper());
        } else {
            sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                  "FROM " + TABLE_NAME + " " +
                  "ORDER BY embedding <=> ?::vector " +
                  "LIMIT ?";

            log.debug("Executing vector search SQL");
            return jdbcTemplate.query(sql,
                    new Object[]{embeddingString, topK},
                    new ChunkRowMapper());
        }
    }

    @Override
    @Transactional
    public void saveChunk(DocumentChunk chunk) {
        log.debug("Saving chunk to vector store: id={}, documentId={}",
                  chunk.getId(), chunk.getDocumentId());

        String embeddingString = arrayToPostgresString(chunk.getEmbedding());
        String metadataJson = metadataToJson(chunk.getMetadata());

        // Use parameterized query for content to prevent SQL injection
        String sql = "INSERT INTO " + TABLE_NAME +
                " (id, document_id, content, chunk_index, embedding, metadata, created_at) " +
                "VALUES (?, ?, ?, ?, ?::vector, " + metadataJson + ", ?) " +
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
                Timestamp.from(chunk.getCreatedAt()));

        log.info("Chunk saved to vector store: id={}", chunk.getId());
    }

    private String arrayToPostgresString(float[] array) {
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

    private String metadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "NULL";
        }
        try {
            return "'" + objectMapper.writeValueAsString(metadata).replace("'", "''") + "'";
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata to JSON", e);
            return "NULL";
        }
    }

    class ChunkRowMapper implements RowMapper<DocumentChunk> {
        @Override
        public DocumentChunk mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = UUID.fromString(rs.getString("id"));
            UUID documentId = UUID.fromString(rs.getString("document_id"));
            String content = rs.getString("content");
            int chunkIndex = rs.getInt("chunk_index");
            
            float[] embedding = parsePostgresVector(rs.getString("embedding"));
            
            Map<String, Object> metadata = parseMetadata(rs.getString("metadata"));
            
            Instant createdAt = parseTimestamp(rs.getString("created_at"));

            return new DocumentChunk(id, documentId, content, chunkIndex, metadata)
                    .withEmbedding(embedding);
        }

        private float[] parsePostgresVector(String vectorString) {
            if (vectorString == null || vectorString.isEmpty()) {
                return new float[0];
            }
            
            String cleaned = vectorString.replace("[", "").replace("]", "");
            String[] parts = cleaned.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        }

        private Map<String, Object> parseMetadata(String metadataJson) {
            Map<String, Object> metadata = new HashMap<>();
            if (metadataJson == null || metadataJson.isEmpty() || "null".equals(metadataJson)) {
                return metadata;
            }
            try {
                metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to parse metadata JSON", e);
            }
            return metadata;
        }

        private Instant parseTimestamp(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) {
                return Instant.now();
            }
            try {
                return Instant.parse(timestamp);
            } catch (DateTimeParseException e) {
                try {
                    return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
                } catch (DateTimeParseException e2) {
                    try {
                        return java.time.LocalDateTime.parse(timestamp.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atZone(java.time.ZoneId.systemDefault()).toInstant();
                    } catch (DateTimeParseException e3) {
                        log.warn("Failed to parse timestamp '{}', using current time", timestamp);
                        return Instant.now();
                    }
                }
            }
        }
    }
}
