package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.DocumentId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps database rows to DocumentChunk domain objects.
 */
public class ChunkRowMapper implements RowMapper<DocumentChunk> {

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
        Instant createdAt = Instant.parse(rs.getString("created_at"));

        return DocumentChunk.reconstitute(id, documentId, content, chunkIndex, metadata, embedding, createdAt);
    }

    private float[] parsePostgresVector(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) return new float[0];
        String cleaned = vectorString.replace("[", "").replace("]", "");
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty() || "null".equals(metadataJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
