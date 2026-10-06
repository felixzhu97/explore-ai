package com.ai.rag.infra.vector;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.ChunkId;
import com.ai.rag.domain.vo.DocumentId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;

/** Maps database rows to DocumentChunk domain objects. */
@RequiredArgsConstructor
public class ChunkRowMapper implements RowMapper<DocumentChunk> {

  private final ObjectMapper objectMapper;

  @Override
  public DocumentChunk mapRow(ResultSet rs, int rowNum) throws SQLException {
    ChunkId id = ChunkId.of(rs.getString("id"));
    DocumentId documentId = DocumentId.of(rs.getString("document_id"));
    OwnerKey ownerKey = OwnerKey.parse(rs.getString("owner_key"));
    String content = rs.getString("content");
    int chunkIndex = rs.getInt("chunk_index");
    float[] embedding = parsePostgresVector(rs.getString("embedding"));
    Map<String, Object> metadata = parseMetadata(rs.getString("metadata"));
    Instant storedAt = rs.getObject("created_at", Instant.class);
    Instant createdAt = storedAt != null ? storedAt : Instant.now();

    return DocumentChunk.reconstitute(
        id, documentId, ownerKey, content, chunkIndex, metadata, embedding, createdAt);
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
