package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.DocumentId;
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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * PgVector adapter for vector search.
 * Uses JDBC for raw SQL operations against PostgreSQL with pgvector extension.
 */
@Component
public class PgVectorAdapter {

    private static final Logger log = LoggerFactory.getLogger(PgVectorAdapter.class);

    private static final String TABLE_NAME = "document_chunks";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PgVectorAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<DocumentChunk> search(float[] queryEmbedding, int topK) {
        return search(queryEmbedding, topK, null);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> search(float[] queryEmbedding, int topK, List<UUID> docIds) {
        String embeddingString = arrayToPostgresString(queryEmbedding);

        String sql;
        if (docIds != null && !docIds.isEmpty()) {
            sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                  "FROM " + TABLE_NAME + " " +
                  "WHERE document_id = ANY(?) " +
                  "ORDER BY embedding <=> ?::vector " +
                  "LIMIT ?";

            return jdbcTemplate.query(sql,
                    new Object[]{docIds.toArray(new UUID[0]), embeddingString, topK},
                    new ChunkRowMapper(objectMapper));
        } else {
            sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                  "FROM " + TABLE_NAME + " " +
                  "ORDER BY embedding <=> ?::vector " +
                  "LIMIT ?";

            return jdbcTemplate.query(sql,
                    new Object[]{embeddingString, topK},
                    new ChunkRowMapper(objectMapper));
        }
    }

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

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata to JSON", e);
            return null;
        }
    }

    public static class ChunkRowMapper implements RowMapper<DocumentChunk> {
        private final ObjectMapper objectMapper;

        public ChunkRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public DocumentChunk mapRow(ResultSet rs, int rowNum) throws SQLException {
            DocumentId id = DocumentId.of(UUID.fromString(rs.getString("id")));
            DocumentId documentId = DocumentId.of(UUID.fromString(rs.getString("document_id")));
            String content = rs.getString("content");
            int chunkIndex = rs.getInt("chunk_index");

            float[] embedding = parsePostgresVector(rs.getString("embedding"));

            Map<String, Object> metadata = parseMetadata(rs.getString("metadata"));

            Instant createdAt = parseTimestamp(rs.getString("created_at"));

            return DocumentChunk.reconstitute(id, documentId, content, chunkIndex, metadata, embedding, createdAt);
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

            List<DateTimeFormatter> formatters = List.of(
                    DateTimeFormatter.ISO_INSTANT,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return Instant.from(formatter.parse(timestamp));
                } catch (DateTimeParseException e) {
                    // try next formatter
                }
            }

            // last resort: replace space with T for local datetime
            try {
                return java.time.LocalDateTime.parse(timestamp.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse timestamp '{}', using current time", timestamp);
                return Instant.now();
            }
        }
    }
}
