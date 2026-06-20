package com.ai.modules.rag.infrastructure.vector;

import com.ai.modules.rag.domain.model.DocumentChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.*;

@DisplayName("PgVectorAdapter Tests")
class PgVectorAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private PgVectorAdapter pgVectorAdapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();
        pgVectorAdapter = new PgVectorAdapter(jdbcTemplate, objectMapper);
    }

    private DocumentChunk createTestChunk(UUID id, UUID documentId, String content) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        return new DocumentChunk(id, documentId, content, 0, metadata);
    }

    private DocumentChunk createTestChunkWithEmbedding(UUID id, UUID documentId, String content, float[] embedding) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        DocumentChunk chunk = new DocumentChunk(id, documentId, content, 0, metadata);
        return chunk.withEmbedding(embedding);
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("should return empty list when no results found")
        void shouldReturnEmptyList_whenNoResultsFound() {
            // Given
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            int topK = 5;
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<DocumentChunk> result = pgVectorAdapter.search(queryEmbedding, topK);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcTemplate).query(
                    contains("SELECT"),
                    eq(new Object[]{"[0.1,0.2,0.3]", topK}),
                    any(RowMapper.class)
            );
        }

        @Test
        @DisplayName("should return chunks when results found")
        void shouldReturnChunks_whenResultsFound() {
            // Given
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            int topK = 5;
            UUID chunkId = UUID.randomUUID();
            UUID docId = UUID.randomUUID();
            String content = "Test content";
            float[] embedding = {0.1f, 0.2f, 0.3f};

            DocumentChunk expectedChunk = createTestChunkWithEmbedding(chunkId, docId, content, embedding);

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of(expectedChunk));

            // When
            List<DocumentChunk> result = pgVectorAdapter.search(queryEmbedding, topK);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("should filter by document IDs when provided")
        void shouldFilterByDocumentIds_whenProvided() {
            // Given
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            int topK = 5;
            List<UUID> docIds = List.of(UUID.randomUUID(), UUID.randomUUID());

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<DocumentChunk> result = pgVectorAdapter.search(queryEmbedding, topK, docIds);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcTemplate).query(
                    contains("WHERE document_id = ANY"),
                    any(Object[].class),
                    any(RowMapper.class)
            );
        }

        @Test
        @DisplayName("should use simple search when document IDs is empty")
        void shouldUseSimpleSearch_whenDocumentIdsIsEmpty() {
            // Given
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            int topK = 5;
            List<UUID> docIds = Collections.emptyList();

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<DocumentChunk> result = pgVectorAdapter.search(queryEmbedding, topK, docIds);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcTemplate).query(
                    not(contains("WHERE document_id = ANY")),
                    any(Object[].class),
                    any(RowMapper.class)
            );
        }

        @Test
        @DisplayName("should use simple search when document IDs is null")
        void shouldUseSimpleSearch_whenDocumentIdsIsNull() {
            // Given
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            int topK = 5;

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<DocumentChunk> result = pgVectorAdapter.search(queryEmbedding, topK, null);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcTemplate).query(
                    not(contains("WHERE document_id = ANY")),
                    any(Object[].class),
                    any(RowMapper.class)
            );
        }
    }

    @Nested
    @DisplayName("SaveChunk Tests")
    class SaveChunkTests {

        @Test
        @DisplayName("should save chunk successfully")
        void shouldSaveChunk_successfully() {
            // Given
            UUID chunkId = UUID.randomUUID();
            UUID documentId = UUID.randomUUID();
            String content = "Test content";
            float[] embedding = {0.1f, 0.2f, 0.3f};
            DocumentChunk chunk = createTestChunkWithEmbedding(chunkId, documentId, content, embedding);

            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            // When
            pgVectorAdapter.saveChunk(chunk);

            // Then
            verify(jdbcTemplate).update(
                    contains("INSERT INTO"),
                    eq(chunkId),
                    eq(documentId),
                    eq(content),
                    eq(0),
                    eq("[0.1,0.2,0.3]"),
                    anyString()
            );
        }

        @Test
        @DisplayName("should save chunk with null metadata")
        void shouldSaveChunk_withNullMetadata() {
            // Given
            UUID chunkId = UUID.randomUUID();
            UUID documentId = UUID.randomUUID();
            String content = "Test content";
            float[] embedding = {0.5f, 0.6f};
            DocumentChunk chunk = new DocumentChunk(chunkId, documentId, content, 0, null)
                    .withEmbedding(embedding);

            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            // When
            pgVectorAdapter.saveChunk(chunk);

            // Then
            verify(jdbcTemplate).update(
                    contains("NULL"),
                    eq(chunkId),
                    eq(documentId),
                    eq(content),
                    eq(0),
                    eq("[0.5,0.6]"),
                    anyString()
            );
        }

        @Test
        @DisplayName("should save chunk with empty metadata")
        void shouldSaveChunk_withEmptyMetadata() {
            // Given
            UUID chunkId = UUID.randomUUID();
            UUID documentId = UUID.randomUUID();
            String content = "Test content";
            float[] embedding = {1.0f};
            DocumentChunk chunk = new DocumentChunk(chunkId, documentId, content, 0, Collections.emptyMap())
                    .withEmbedding(embedding);

            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            // When
            pgVectorAdapter.saveChunk(chunk);

            // Then
            verify(jdbcTemplate).update(
                    contains("NULL"),
                    eq(chunkId),
                    eq(documentId),
                    eq(content),
                    eq(0),
                    eq("[1.0]"),
                    anyString()
            );
        }

        @Test
        @DisplayName("should escape single quotes in metadata JSON")
        void shouldEscapeSingleQuotes_inMetadataJson() throws Exception {
            // Given
            UUID chunkId = UUID.randomUUID();
            UUID documentId = UUID.randomUUID();
            String content = "Test";
            float[] embedding = {0.1f};
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("quote", "O'Reilly");
            DocumentChunk chunk = new DocumentChunk(chunkId, documentId, content, 0, metadata)
                    .withEmbedding(embedding);

            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            // When
            pgVectorAdapter.saveChunk(chunk);

            // Then
            verify(jdbcTemplate).update(
                    argThat(sql -> sql.contains("O''Reilly")),
                    any(Object[].class)
            );
        }
    }

    @Nested
    @DisplayName("ArrayToPostgresString Tests")
    class ArrayToPostgresStringTests {

        @Test
        @DisplayName("should convert float array to postgres string")
        void shouldConvertFloatArray_toPostgresString() throws Exception {
            // Given
            float[] embedding = {0.1f, -0.2f, 0.3f, 1.0f};

            // When
            String result = invokePrivateStringMethod("arrayToPostgresString", float[].class, embedding);

            // Then
            assertThat(result).isEqualTo("[0.1,-0.2,0.3,1.0]");
        }

        @Test
        @DisplayName("should handle single element array")
        void shouldHandleSingleElementArray() throws Exception {
            // Given
            float[] embedding = {0.5f};

            // When
            String result = invokePrivateStringMethod("arrayToPostgresString", float[].class, embedding);

            // Then
            assertThat(result).isEqualTo("[0.5]");
        }

        @Test
        @DisplayName("should handle empty array")
        void shouldHandleEmptyArray() throws Exception {
            // Given
            float[] embedding = {};

            // When
            String result = invokePrivateStringMethod("arrayToPostgresString", float[].class, embedding);

            // Then
            assertThat(result).isEqualTo("[]");
        }
    }

    @Nested
    @DisplayName("MetadataToJson Tests")
    class MetadataToJsonTests {

        @Test
        @DisplayName("should convert metadata to JSON string")
        void shouldConvertMetadata_toJsonString() throws Exception {
            // Given
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", "value");
            metadata.put("number", 42);

            // When
            String result = invokePrivateStringMethod("metadataToJson", Map.class, metadata);

            // Then
            assertThat(result).contains("key");
            assertThat(result).contains("value");
            assertThat(result).contains("42");
        }

        @Test
        @DisplayName("should return NULL for null metadata")
        void shouldReturnNull_forNullMetadata() throws Exception {
            // When
            String result = invokePrivateStringMethod("metadataToJson", Map.class, (Object) null);

            // Then
            assertThat(result).isEqualTo("NULL");
        }

        @Test
        @DisplayName("should return NULL for empty metadata")
        void shouldReturnNull_forEmptyMetadata() throws Exception {
            // Given
            Map<String, Object> metadata = Collections.emptyMap();

            // When
            String result = invokePrivateStringMethod("metadataToJson", Map.class, metadata);

            // Then
            assertThat(result).isEqualTo("NULL");
        }

        @Test
        @DisplayName("should return NULL when JSON serialization fails")
        void shouldReturnNull_whenJsonSerializationFails() throws Exception {
            // Given
            ObjectMapper failingMapper = new ObjectMapper();
            PgVectorAdapter adapterWithFailingMapper = new PgVectorAdapter(jdbcTemplate, failingMapper);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("circular", new CircularReference());

            // When
            String result = invokePrivateStringMethod(adapterWithFailingMapper, "metadataToJson", Map.class, metadata);

            // Then
            assertThat(result).isEqualTo("NULL");
        }
    }

    @Nested
    @DisplayName("ChunkRowMapper Tests")
    class ChunkRowMapperTests {

        @Test
        @DisplayName("should map result set to document chunk")
        void shouldMapResultSet_toDocumentChunk() throws SQLException {
            // Given
            UUID chunkId = UUID.randomUUID();
            UUID documentId = UUID.randomUUID();
            String content = "Test content";
            int chunkIndex = 1;
            String embedding = "[0.1,0.2,0.3]";
            String metadata = "{\"source\":\"test\"}";
            String createdAt = "2024-01-01T00:00:00Z";

            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(chunkId.toString());
            when(rs.getString("document_id")).thenReturn(documentId.toString());
            when(rs.getString("content")).thenReturn(content);
            when(rs.getInt("chunk_index")).thenReturn(chunkIndex);
            when(rs.getString("embedding")).thenReturn(embedding);
            when(rs.getString("metadata")).thenReturn(metadata);
            when(rs.getString("created_at")).thenReturn(createdAt);

            PgVectorAdapter.ChunkRowMapper rowMapper = pgVectorAdapter.new ChunkRowMapper();

            // When
            DocumentChunk result = rowMapper.mapRow(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(chunkId);
            assertThat(result.getDocumentId()).isEqualTo(documentId);
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getChunkIndex()).isEqualTo(chunkIndex);
            assertThat(result.getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
            assertThat(result.getMetadata()).containsEntry("source", "test");
        }

        @Test
        @DisplayName("should handle null embedding")
        void shouldHandleNullEmbedding() throws SQLException {
            // Given
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("document_id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("content")).thenReturn("Test");
            when(rs.getInt("chunk_index")).thenReturn(0);
            when(rs.getString("embedding")).thenReturn(null);
            when(rs.getString("metadata")).thenReturn(null);
            when(rs.getString("created_at")).thenReturn(null);

            PgVectorAdapter.ChunkRowMapper rowMapper = pgVectorAdapter.new ChunkRowMapper();

            // When
            DocumentChunk result = rowMapper.mapRow(rs, 0);

            // Then
            assertThat(result.getEmbedding()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty embedding string")
        void shouldHandleEmptyEmbeddingString() throws SQLException {
            // Given
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("document_id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("content")).thenReturn("Test");
            when(rs.getInt("chunk_index")).thenReturn(0);
            when(rs.getString("embedding")).thenReturn("");
            when(rs.getString("metadata")).thenReturn(null);
            when(rs.getString("created_at")).thenReturn(null);

            PgVectorAdapter.ChunkRowMapper rowMapper = pgVectorAdapter.new ChunkRowMapper();

            // When
            DocumentChunk result = rowMapper.mapRow(rs, 0);

            // Then
            assertThat(result.getEmbedding()).isEmpty();
        }

        @Test
        @DisplayName("should use current time when created_at is null")
        void shouldUseCurrentTime_whenCreatedAtIsNull() throws SQLException {
            // Given
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("document_id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("content")).thenReturn("Test");
            when(rs.getInt("chunk_index")).thenReturn(0);
            when(rs.getString("embedding")).thenReturn("[0.1]");
            when(rs.getString("metadata")).thenReturn(null);
            when(rs.getString("created_at")).thenReturn(null);

            Instant beforeTest = Instant.now();
            PgVectorAdapter.ChunkRowMapper rowMapper = pgVectorAdapter.new ChunkRowMapper();

            // When
            DocumentChunk result = rowMapper.mapRow(rs, 0);

            // Then
            assertThat(result.getCreatedAt()).isAfterOrEqualTo(beforeTest);
        }

    }

    // Helper method to invoke private methods via reflection
    private String invokePrivateStringMethod(String methodName, Class<?> paramType, Object param) throws Exception {
        var method = PgVectorAdapter.class.getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        return (String) method.invoke(pgVectorAdapter, param);
    }

    private String invokePrivateStringMethod(PgVectorAdapter adapter, String methodName, Class<?> paramType, Object param) throws Exception {
        var method = PgVectorAdapter.class.getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        return (String) method.invoke(adapter, param);
    }

    // Helper class for testing JSON serialization failure
    private static class CircularReference {
        private final CircularReference self = this;
    }
}
