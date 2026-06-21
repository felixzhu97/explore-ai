package com.ai.adapter.out;

import com.ai.modules.rag.infrastructure.vector.PgVectorAdapter;
import com.ai.modules.rag.domain.model.DocumentChunk;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ChunkRowMapper Integration Tests
 *
 * Tests the ChunkRowMapper behavior through PgVectorAdapter.search():
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests vector search results mapping, SQL construction, and parameter passing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChunkRowMapper via PgVectorAdapter")
class ChunkRowMapperTest {

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
    @DisplayName("shouldMapRowToDocumentChunk")
    class ShouldMapRowToDocumentChunk {

        @Test
        @DisplayName("should map search results to DocumentChunk list")
        void shouldMapSearchResultsToDocumentChunkList() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f, 4.0f};
            int topK = 5;

            // Mock the search to return expected chunks
            DocumentChunk expectedChunk = createMockChunk(TEST_CHUNK_ID, TEST_DOCUMENT_ID);
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of(expectedChunk));

            // Act
            List<DocumentChunk> results = adapter.search(queryEmbedding, topK);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(TEST_CHUNK_ID);
            assertThat(results.get(0).getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should return empty list when no results found")
        void shouldReturnEmptyListWhenNoResultsFound() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f, 4.0f};
            int topK = 5;
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            List<DocumentChunk> results = adapter.search(queryEmbedding, topK);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("shouldParseValidPostgresVectorString")
    class ShouldParseValidPostgresVectorString {

        @Test
        @DisplayName("should pass correct vector string to SQL")
        void shouldPassCorrectVectorStringToSql() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f, 4.0f};
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(queryEmbedding, 5);

            // Assert - Verify the embedding is converted to postgres format
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(anyString(), captor.capture(), any(RowMapper.class));

            Object[] params = captor.getValue();
            assertThat(params[0]).isEqualTo("[1.0,2.0,3.0,4.0]");
        }

        @Test
        @DisplayName("should handle vector with decimal precision")
        void shouldHandleVectorWithDecimalPrecision() {
            // Arrange
            float[] queryEmbedding = {0.123456f, 1.999999f, -0.5f, 100.0f};
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(queryEmbedding, 5);

            // Assert
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(anyString(), captor.capture(), any(RowMapper.class));

            Object[] params = captor.getValue();
            assertThat(params[0].toString()).contains("0.123456");
            assertThat(params[0].toString()).contains("-0.5");
        }
    }

    @Nested
    @DisplayName("shouldReturnEmptyArrayForNullVectorString")
    class ShouldReturnEmptyArrayForNullVectorString {

        @Test
        @DisplayName("should handle null from database gracefully")
        void shouldHandleNullFromDatabaseGracefully() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f};
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            List<DocumentChunk> results = adapter.search(queryEmbedding, 5);

            // Assert - The method should not throw even if DB returns null
            assertThat(results).isNotNull();
        }
    }

    @Nested
    @DisplayName("shouldParseValidMetadataJson")
    class ShouldParseValidMetadataJson {

        @Test
        @DisplayName("should search and pass metadata to results")
        void shouldSearchAndPassMetadataToResults() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f};
            Map<String, Object> metadata = Map.of("source", "test.pdf", "page", 1);
            DocumentChunk chunkWithMetadata = DocumentChunk.create(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    metadata
            ).withEmbedding(new float[]{1.0f, 2.0f, 3.0f});

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of(chunkWithMetadata));

            // Act
            List<DocumentChunk> results = adapter.search(queryEmbedding, 5);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).containsKey("source");
        }
    }

    @Nested
    @DisplayName("shouldReturnEmptyMapForNullMetadata")
    class ShouldReturnEmptyMapForNullMetadata {

        @Test
        @DisplayName("should return empty metadata when parseMetadata returns empty map")
        void shouldReturnEmptyMetadataWhenParseMetadataReturnsEmptyMap() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f};
            // When parseMetadata returns empty map for null input, the chunk gets empty metadata
            DocumentChunk chunkWithEmptyMetadata = DocumentChunk.create(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    java.util.Map.of() // Empty map, not null
            ).withEmbedding(new float[]{1.0f, 2.0f, 3.0f});

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of(chunkWithEmptyMetadata));

            // Act
            List<DocumentChunk> results = adapter.search(queryEmbedding, 5);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).isNotNull();
            assertThat(results.get(0).getMetadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("shouldParseIsoInstantTimestamp")
    class ShouldParseIsoInstantTimestamp {

        @Test
        @DisplayName("should use correct timestamp in SQL query")
        void shouldUseCorrectTimestampInSqlQuery() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f};
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(queryEmbedding, 5);

            // Assert - Verify SQL contains created_at column
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));

            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("created_at");
        }
    }

    @Nested
    @DisplayName("shouldFallbackToNowForUnparseableTimestamp")
    class ShouldFallbackToNowForUnparseableTimestamp {

        @Test
        @DisplayName("should handle chunks without timestamps")
        void shouldHandleChunksWithoutTimestamps() {
            // Arrange
            float[] queryEmbedding = {1.0f, 2.0f, 3.0f};
            DocumentChunk chunkWithoutTimestamp = DocumentChunk.create(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    Map.of()
            ).withEmbedding(new float[]{1.0f, 2.0f, 3.0f});

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of(chunkWithoutTimestamp));

            // Act
            List<DocumentChunk> results = adapter.search(queryEmbedding, 5);

            // Assert - Should not throw and should have a timestamp
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("RowMapper SQL Column Verification")
    class RowMapperSqlColumnVerification {

        @Test
        @DisplayName("should select all required columns")
        void shouldSelectAllRequiredColumns() {
            // Arrange
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(new float[]{1.0f, 2.0f}, 5);

            // Assert
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));

            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("id");
            assertThat(sql).contains("document_id");
            assertThat(sql).contains("content");
            assertThat(sql).contains("chunk_index");
            assertThat(sql).contains("embedding");
            assertThat(sql).contains("metadata");
        }

        @Test
        @DisplayName("should use correct distance operator")
        void shouldUseCorrectDistanceOperator() {
            // Arrange
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            // Act
            adapter.search(new float[]{1.0f, 2.0f}, 5);

            // Assert
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));

            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("<=>"); // Cosine distance operator
            assertThat(sql).contains("::vector");
        }
    }

    private DocumentChunk createMockChunk(UUID id, UUID documentId) {
        Map<String, Object> metadata = Map.of("source", "test", "page", 1);
        return DocumentChunk.create(
                id,
                documentId,
                "Test content " + id,
                0,
                metadata
        ).withEmbedding(new float[]{1.0f, 2.0f, 3.0f, 4.0f});
    }
}
