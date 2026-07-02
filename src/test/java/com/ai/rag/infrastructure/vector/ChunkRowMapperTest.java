package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.DocumentId;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChunkRowMapper via H2VectorAdapter")
class ChunkRowMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private H2VectorAdapter adapter;

    private static final DocumentId TEST_DOCUMENT_ID = DocumentId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    private static final DocumentId TEST_CHUNK_ID = DocumentId.of(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        adapter = new H2VectorAdapter(jdbcTemplate, objectMapper);
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        void shouldMapSearchResults() {
            DocumentChunk expectedChunk = createMockChunk(TEST_CHUNK_ID, TEST_DOCUMENT_ID);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenReturn(List.of(expectedChunk));
            List<DocumentChunk> results = adapter.search(new float[]{1.0f, 0.0f, 0.0f, 0.0f}, 5);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(TEST_CHUNK_ID);
            assertThat(results.get(0).getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
            assertThat(results.get(0).getContent()).isEqualTo("Test content " + TEST_CHUNK_ID);
            assertThat(results.get(0).getEmbedding()).containsExactly(1.0f, 2.0f, 3.0f, 4.0f);
        }

        @Test
        void shouldReturnEmptyList() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenReturn(List.of());
            List<DocumentChunk> results = adapter.search(new float[]{1.0f, 2.0f}, 5);
            assertThat(results).isEmpty();
        }

        @Test
        void shouldRankByCosineSimilarity() {
            DocumentChunk lowScore = DocumentChunk.create(
                    TEST_CHUNK_ID, TEST_DOCUMENT_ID, "low", 0, Map.of())
                    .withEmbedding(new float[]{0.0f, 1.0f});
            DocumentChunk highScore = DocumentChunk.create(
                    DocumentId.generate(), TEST_DOCUMENT_ID, "high", 1, Map.of())
                    .withEmbedding(new float[]{1.0f, 0.0f});
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenReturn(List.of(lowScore, highScore));

            List<DocumentChunk> results = adapter.search(new float[]{1.0f, 0.0f}, 1);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContent()).isEqualTo("high");
        }

        @Test
        void shouldSelectAllRequiredColumns() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenReturn(List.of());
            adapter.search(new float[]{1.0f, 2.0f}, 5);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
            String sql = sqlCaptor.getValue();
            assertThat(sql).contains("id");
            assertThat(sql).contains("document_id");
            assertThat(sql).contains("content");
            assertThat(sql).contains("embedding");
            assertThat(sql).contains("metadata");
            assertThat(sql).contains("created_at");
        }

        @Test
        void shouldPassMetadataToResults() {
            Map<String, Object> metadata = Map.of("source", "test.pdf", "page", 1);
            DocumentChunk chunkWithMetadata = DocumentChunk.create(
                    TEST_CHUNK_ID, TEST_DOCUMENT_ID, "Test content", 0, metadata)
                    .withEmbedding(new float[]{1.0f, 2.0f});
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenReturn(List.of(chunkWithMetadata));
            List<DocumentChunk> results = adapter.search(new float[]{1.0f, 2.0f}, 5);
            assertThat(results.get(0).getMetadata()).containsKey("source");
        }
    }

    private DocumentChunk createMockChunk(DocumentId id, DocumentId documentId) {
        Map<String, Object> metadata = Map.of("source", "test", "page", 1);
        return DocumentChunk.create(id, documentId, "Test content " + id, 0, metadata)
                .withEmbedding(new float[]{1.0f, 2.0f, 3.0f, 4.0f});
    }
}
