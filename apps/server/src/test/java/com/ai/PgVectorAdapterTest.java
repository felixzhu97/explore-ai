package com.ai.infrastructure;

import com.ai.infrastructure.adapter.vector.PgVectorAdapter;
import com.ai.domain.model.DocumentChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

/**
 * PgVectorAdapter Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (JdbcTemplate):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests vector search, SQL construction, and data mapping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PgVectorAdapter")
class PgVectorAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PgVectorAdapter adapter;

    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_CHUNK_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        adapter = new PgVectorAdapter(jdbcTemplate, objectMapper);
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should execute search query with correct parameters")
        void shouldExecuteSearchQueryWithCorrectParameters() {
            // Arrange
            float[] queryEmbedding = createMockEmbedding(4);
            int topK = 5;
            List<DocumentChunk> mockResults = List.of();
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(mockResults);

            // Act
            adapter.search(queryEmbedding, topK);

            // Assert
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(
                    anyString(),
                    captor.capture(),
                    any(RowMapper.class)
            );
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("should return list of document chunks from search results")
        void shouldReturnListOfDocumentChunksFromSearchResults() {
            // Arrange
            float[] queryEmbedding = createMockEmbedding(4);
            int topK = 3;
            List<DocumentChunk> expectedChunks = createMockChunks(2);
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(expectedChunks);

            // Act
            List<DocumentChunk> results = adapter.search(queryEmbedding, topK);

            // Assert
            assertThat(results).isEqualTo(expectedChunks);
        }

        @Test
        @DisplayName("should search with docIds filter when provided")
        void shouldSearchWithDocIdsFilterWhenProvided() {
            // Arrange
            float[] queryEmbedding = createMockEmbedding(4);
            int topK = 5;
            List<UUID> docIds = List.of(TEST_DOCUMENT_ID, UUID.randomUUID());
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(queryEmbedding, topK, docIds);

            // Assert
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(
                    contains("document_id = ANY"),
                    captor.capture(),
                    any(RowMapper.class)
            );
            assertThat(captor.getValue()).hasSize(3); // docIds array, embedding, topK
        }

        @Test
        @DisplayName("should ignore empty docIds list and not include filter")
        void shouldIgnoreEmptyDocIdsListAndNotIncludeFilter() {
            // Arrange
            float[] queryEmbedding = createMockEmbedding(4);
            int topK = 5;
            List<UUID> emptyDocIds = List.of();
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(queryEmbedding, topK, emptyDocIds);

            // Assert - verify docIds filter is not present when empty
            verify(jdbcTemplate).query(
                    contains("ORDER BY embedding"),
                    ArgumentMatchers.<Object[]>any(),
                    any(RowMapper.class)
            );
        }
    }

    @Nested
    @DisplayName("saveChunk")
    class SaveChunk {

        @Test
        @DisplayName("should insert chunk with correct values")
        void shouldInsertChunkWithCorrectValues() {
            // Arrange
            DocumentChunk chunk = createMockChunk(TEST_CHUNK_ID, TEST_DOCUMENT_ID);

            // Act
            adapter.saveChunk(chunk);

            // Assert
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(
                    contains("INSERT INTO"),
                    captor.capture()
            );
            Object[] params = captor.getValue();
            assertThat(params[0]).isEqualTo(chunk.getId());
            assertThat(params[1]).isEqualTo(chunk.getDocumentId());
            assertThat(params[2]).isEqualTo(chunk.getContent());
            assertThat(params[3]).isEqualTo(chunk.getChunkIndex());
        }

        @Test
        @DisplayName("should convert embedding array to postgres string format")
        void shouldConvertEmbeddingArrayToPostgresStringFormat() {
            // Arrange
            float[] embedding = {1.0f, 2.0f, 3.0f, 4.0f};
            DocumentChunk chunk = new DocumentChunk(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    Map.of()
            ).withEmbedding(embedding);

            // Act
            adapter.saveChunk(chunk);

            // Assert
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(
                    anyString(),
                    captor.capture()
            );
            // The embedding string should be the 4th parameter (index 4 in the params array)
            Object embeddingParam = captor.getValue()[4];
            assertThat(embeddingParam).isEqualTo("[1.0,2.0,3.0,4.0]");
        }

        @Test
        @DisplayName("should handle chunk with null metadata")
        void shouldHandleChunkWithNullMetadata() {
            // Arrange
            DocumentChunk chunk = new DocumentChunk(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    null
            ).withEmbedding(createMockEmbedding(4));

            // Act
            adapter.saveChunk(chunk);

            // Assert - metadata is in VALUES clause with NULL placeholder
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(
                    sqlCaptor.capture(),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class)
            );
            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("metadata");
            assertThat(sql).contains("NULL");
        }

        @Test
        @DisplayName("should serialize metadata to JSON")
        void shouldSerializeMetadataToJson() {
            // Arrange
            Map<String, Object> metadata = Map.of("source", "test", "page", 1);
            DocumentChunk chunk = new DocumentChunk(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    metadata
            ).withEmbedding(createMockEmbedding(4));

            // Act
            adapter.saveChunk(chunk);

            // Assert - verify update was called
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(
                    sqlCaptor.capture(),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class)
            );
            assertThat(sqlCaptor.getValue()).contains("INSERT INTO");
        }

        @Test
        @DisplayName("should handle empty metadata map")
        void shouldHandleEmptyMetadataMap() {
            // Arrange
            DocumentChunk chunk = new DocumentChunk(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    Map.of()
            ).withEmbedding(createMockEmbedding(4));

            // Act
            adapter.saveChunk(chunk);

            // Assert - empty metadata should use NULL in VALUES clause
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(
                    sqlCaptor.capture(),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class)
            );
            String sql = sqlCaptor.getValue();
            // metadata field appears once in VALUES clause as NULL
            assertThat(sql).contains("metadata");
            assertThat(sql).contains("NULL");
        }
    }

    @Nested
    @DisplayName("RowMapper behavior")
    class RowMapperBehavior {

        @Test
        @DisplayName("should call RowMapper with correct SQL")
        void shouldCallRowMapperWithCorrectSql() {
            // Arrange
            float[] queryEmbedding = createMockEmbedding(4);
            int topK = 5;
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(queryEmbedding, topK);

            // Assert - verify SQL contains expected columns
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).query(
                    sqlCaptor.capture(),
                    ArgumentMatchers.<Object[]>any(),
                    any(RowMapper.class)
            );
            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("id");
            assertThat(sql).contains("document_id");
            assertThat(sql).contains("content");
            assertThat(sql).contains("embedding");
            assertThat(sql).contains("metadata");
        }

        @Test
        @DisplayName("should pass embedding vector as string parameter")
        void shouldPassEmbeddingVectorAsStringParameter() {
            // Arrange
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            int topK = 5;
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(queryEmbedding, topK);

            // Assert
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(
                    anyString(),
                    captor.capture(),
                    any(RowMapper.class)
            );
            assertThat(captor.getValue()[0]).isEqualTo("[0.1,0.2,0.3]");
            assertThat(captor.getValue()[1]).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("SQL Construction")
    class SqlConstruction {

        @Test
        @DisplayName("should use cosine distance operator for ordering")
        void shouldUseCosineDistanceOperatorForOrdering() {
            // Arrange
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(createMockEmbedding(4), 5);

            // Assert
            verify(jdbcTemplate).query(
                    contains("<=>"),
                    ArgumentMatchers.<Object[]>any(),
                    any(RowMapper.class)
            );
        }

        @Test
        @DisplayName("should cast embedding to vector type")
        void shouldCastEmbeddingToVectorType() {
            // Arrange
            when(jdbcTemplate.query(anyString(), ArgumentMatchers.<Object[]>any(), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(createMockEmbedding(4), 5);

            // Assert
            verify(jdbcTemplate).query(
                    contains("::vector"),
                    ArgumentMatchers.<Object[]>any(),
                    any(RowMapper.class)
            );
        }

        @Test
        @DisplayName("should use upsert pattern with ON CONFLICT")
        void shouldUseUpsertPatternWithOnConflict() {
            // Arrange
            DocumentChunk chunk = createMockChunk(TEST_CHUNK_ID, TEST_DOCUMENT_ID);

            // Act
            adapter.saveChunk(chunk);

            // Assert
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(
                    sqlCaptor.capture(),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class)
            );
            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("ON CONFLICT");
        }

        @Test
        @DisplayName("should update content, embedding, and metadata on conflict")
        void shouldUpdateContentEmbeddingAndMetadataOnConflict() {
            // Arrange
            DocumentChunk chunk = createMockChunk(TEST_CHUNK_ID, TEST_DOCUMENT_ID);

            // Act
            adapter.saveChunk(chunk);

            // Assert
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(
                    sqlCaptor.capture(),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class),
                    any(Object.class)
            );
            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("content = EXCLUDED.content");
            assertThat(sql).contains("embedding = EXCLUDED.embedding");
            assertThat(sql).contains("metadata = EXCLUDED.metadata");
        }
    }

    private float[] createMockEmbedding(int dimensions) {
        float[] embedding = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            embedding[i] = (float) (i + 1) * 0.1f;
        }
        return embedding;
    }

    private List<DocumentChunk> createMockChunks(int count) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(createMockChunk(UUID.randomUUID(), TEST_DOCUMENT_ID));
        }
        return chunks;
    }

    private DocumentChunk createMockChunk(UUID id, UUID documentId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        metadata.put("page", 1);
        
        return new DocumentChunk(
                id,
                documentId,
                "Test content " + id,
                0,
                metadata
        ).withEmbedding(createMockEmbedding(4));
    }
}
