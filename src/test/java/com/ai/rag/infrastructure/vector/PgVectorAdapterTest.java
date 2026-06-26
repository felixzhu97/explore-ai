package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.DocumentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Nested
    @DisplayName("Search")
    class Search {

        @Test
        @DisplayName("should return empty list when no results")
        void shouldReturnEmptyList() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(Collections.emptyList());
            assertThat(pgVectorAdapter.search(new float[]{0.1f, 0.2f}, 5)).isEmpty();
        }

        @Test
        @DisplayName("should filter by document IDs")
        void shouldFilterByDocumentIds() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(Collections.emptyList());
            List<UUID> docIds = List.of(UUID.randomUUID());
            pgVectorAdapter.search(new float[]{0.1f}, 5, docIds);
            verify(jdbcTemplate).query(contains("WHERE document_id = ANY"),
                    any(RowMapper.class), any(Object[].class));
        }

        @Test
        @DisplayName("should use simple search when docIds is empty")
        void shouldUseSimpleSearchWhenDocIdsIsEmpty() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(Collections.emptyList());
            pgVectorAdapter.search(new float[]{0.1f}, 5, List.of());
            verify(jdbcTemplate, atLeastOnce()).query(contains("SELECT"),
                    any(RowMapper.class), any(Object[].class));
        }
    }

    @Nested
    @DisplayName("SaveChunk")
    class SaveChunk {

        @Test
        @DisplayName("should save chunk with embedding")
        void shouldSaveChunk() {
            DocumentChunk chunk = DocumentChunk.create(
                    DocumentId.generate(), DocumentId.generate(), "content", 0, Map.of())
                    .withEmbedding(new float[]{0.1f, 0.2f});
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
            pgVectorAdapter.saveChunk(chunk);
            verify(jdbcTemplate).update(contains("::jsonb"), any(Object[].class));
        }

        @Test
        @DisplayName("should handle null metadata")
        void shouldHandleNullMetadata() {
            DocumentChunk chunk = DocumentChunk.create(
                    DocumentId.generate(), DocumentId.generate(), "content", 0, null)
                    .withEmbedding(new float[]{0.1f});
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
            pgVectorAdapter.saveChunk(chunk);
            verify(jdbcTemplate).update(anyString(), any(Object[].class));
        }
    }

    @Nested
    @DisplayName("ChunkRowMapper")
    class ChunkRowMapperTests {

        private ChunkRowMapper rowMapper;

        @BeforeEach
        void setUp() {
            rowMapper = new ChunkRowMapper(objectMapper);
        }

        @Test
        @DisplayName("should map result set to document chunk")
        void shouldMapResultSet() throws SQLException {
            UUID chunkId = UUID.randomUUID();
            UUID docId = UUID.randomUUID();
            String content = "Test content";
            String embedding = "[0.1,0.2,0.3]";
            String metadata = "{\"source\":\"test\"}";
            String createdAt = "2024-01-01T00:00:00Z";

            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(chunkId.toString());
            when(rs.getString("document_id")).thenReturn(docId.toString());
            when(rs.getString("content")).thenReturn(content);
            when(rs.getInt("chunk_index")).thenReturn(1);
            when(rs.getString("embedding")).thenReturn(embedding);
            when(rs.getString("metadata")).thenReturn(metadata);
            when(rs.getString("created_at")).thenReturn(createdAt);

            DocumentChunk result = rowMapper.mapRow(rs, 0);

            assertThat(result.getId().value()).isEqualTo(chunkId);
            assertThat(result.getDocumentId().value()).isEqualTo(docId);
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getChunkIndex()).isEqualTo(1);
            assertThat(result.getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
            assertThat(result.getMetadata()).containsEntry("source", "test");
        }

        @Test
        @DisplayName("should handle null embedding")
        void shouldHandleNullEmbedding() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("document_id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("content")).thenReturn("Test");
            when(rs.getInt("chunk_index")).thenReturn(0);
            when(rs.getString("embedding")).thenReturn(null);
            when(rs.getString("metadata")).thenReturn(null);
            when(rs.getString("created_at")).thenReturn("2024-01-01T00:00:00Z");

            DocumentChunk result = rowMapper.mapRow(rs, 0);
            assertThat(result.getEmbedding()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty metadata")
        void shouldHandleEmptyMetadata() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("document_id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("content")).thenReturn("Test");
            when(rs.getInt("chunk_index")).thenReturn(0);
            when(rs.getString("embedding")).thenReturn("[0.1]");
            when(rs.getString("metadata")).thenReturn("{}");
            when(rs.getString("created_at")).thenReturn("2024-01-01T00:00:00Z");

            DocumentChunk result = rowMapper.mapRow(rs, 0);
            assertThat(result.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle invalid metadata JSON gracefully")
        void shouldHandleInvalidMetadata() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("document_id")).thenReturn(UUID.randomUUID().toString());
            when(rs.getString("content")).thenReturn("Test");
            when(rs.getInt("chunk_index")).thenReturn(0);
            when(rs.getString("embedding")).thenReturn("[0.1]");
            when(rs.getString("metadata")).thenReturn("invalid json {");
            when(rs.getString("created_at")).thenReturn("2024-01-01T00:00:00Z");

            DocumentChunk result = rowMapper.mapRow(rs, 0);
            assertThat(result.getMetadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findChunksByDocumentId")
    class FindChunksByDocumentId {

        @Test
        @DisplayName("should find chunks by document ID")
        void shouldFindChunksByDocumentId() {
            DocumentId docId = DocumentId.generate();
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class)))
                    .thenReturn(List.of());
            pgVectorAdapter.findChunksByDocumentId(docId);
            verify(jdbcTemplate).query(contains("WHERE document_id = ?"),
                    any(RowMapper.class), eq(docId.value()));
        }
    }

    @Nested
    @DisplayName("deleteChunksByDocumentId")
    class DeleteChunksByDocumentId {

        @Test
        @DisplayName("should delete chunks by document ID")
        void shouldDeleteChunksByDocumentId() {
            DocumentId docId = DocumentId.generate();
            when(jdbcTemplate.update(anyString(), any(UUID.class))).thenReturn(1);
            pgVectorAdapter.deleteChunksByDocumentId(docId);
            verify(jdbcTemplate).update(contains("DELETE FROM"), eq(docId.value()));
        }
    }
}
