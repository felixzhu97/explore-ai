package com.ai.rag.infra.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.rag.domain.model.ChunkId;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.domain.model.ScoredChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@DisplayName("H2DocumentChunkRepository")
class H2DocumentChunkRepositoryTest {

  private static final OwnerKey OWNER_KEY = OwnerKey.parse("c:owner");

  private JdbcTemplate jdbcTemplate;
  private ObjectMapper objectMapper;
  private H2DocumentChunkRepository chunkRepository;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    objectMapper = new ObjectMapper();
    chunkRepository = new H2DocumentChunkRepository(jdbcTemplate, objectMapper);
  }

  @Nested
  @DisplayName("Search")
  class Search {

    private static final String OWNER = "c:owner";
    private static final String OTHER_OWNER = "c:other";

    private H2DocumentChunkRepository repository;

    @BeforeEach
    void setUpDatabase() {
      JdbcTemplate h2 =
          new JdbcTemplate(
              new DriverManagerDataSource(
                  "jdbc:h2:mem:chunk-search-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                  "sa",
                  ""));
      h2.execute(
          "CREATE TABLE document_chunk (id UUID PRIMARY KEY, document_id UUID,"
              + " content CLOB, chunk_index INT, embedding CLOB, metadata CLOB,"
              + " created_at VARCHAR(40), owner_key VARCHAR(80) NOT NULL)");
      repository = new H2DocumentChunkRepository(h2, objectMapper);
    }

    @Test
    @DisplayName("should return empty list when the owner has no chunks")
    void shouldReturnEmptyListWhenTheOwnerHasNoChunks() {
      save(DocumentId.generate(), OTHER_OWNER, "other", new float[] {1f, 0f});

      assertThat(repository.search(new float[] {1f, 0f}, 5, OWNER, List.of())).isEmpty();
    }

    @Test
    @DisplayName("should rank only the owner chunks by cosine similarity")
    void shouldRankOnlyTheOwnerChunksByCosineSimilarity() {
      save(DocumentId.generate(), OWNER, "low", new float[] {0f, 1f});
      save(DocumentId.generate(), OWNER, "high", new float[] {1f, 0f});
      save(DocumentId.generate(), OTHER_OWNER, "foreign", new float[] {1f, 0f});

      List<ScoredChunk> results = repository.search(new float[] {1f, 0f}, 5, OWNER, List.of());

      assertThat(results)
          .extracting(scored -> scored.chunk().getContent())
          .containsExactly("high", "low");
      assertThat(results).extracting(ScoredChunk::score).containsExactly(1.0, 0.0);
    }

    @Test
    @DisplayName("should ignore another owner document when its id is requested")
    void shouldIgnoreAnotherOwnerDocumentWhenItsIdIsRequested() {
      DocumentId own = DocumentId.generate();
      DocumentId foreign = DocumentId.generate();
      save(own, OWNER, "own", new float[] {1f, 0f});
      save(foreign, OTHER_OWNER, "foreign", new float[] {1f, 0f});

      List<ScoredChunk> results =
          repository.search(
              new float[] {1f, 0f}, 5, OWNER, List.of(own.getValue(), foreign.getValue()));

      assertThat(results).extracting(scored -> scored.chunk().getContent()).containsExactly("own");
    }

    @Test
    @DisplayName("should store the owner in its column and read it back without metadata")
    void shouldStoreTheOwnerInItsColumnAndReadItBackWithoutMetadata() {
      DocumentId documentId = DocumentId.generate();
      save(documentId, OWNER, "own", new float[] {1f, 0f});

      DocumentChunk stored = repository.findChunksByDocumentId(documentId).getFirst();

      assertThat(stored.getOwnerKey()).isEqualTo(OwnerKey.parse(OWNER));
      assertThat(stored.getMetadata()).doesNotContainKey("ownerKey");
    }

    @Test
    @DisplayName("should return the opening chunks of each requested owner document")
    void shouldReturnTheOpeningChunksOfEachRequestedOwnerDocument() {
      final DocumentId first = DocumentId.generate();
      final DocumentId second = DocumentId.generate();
      final DocumentId foreign = DocumentId.generate();
      save(first, OWNER, "first-2", 2);
      save(first, OWNER, "first-0", 0);
      save(first, OWNER, "first-1", 1);
      save(second, OWNER, "second-0", 0);
      save(foreign, OTHER_OWNER, "foreign-0", 0);

      List<DocumentChunk> results =
          repository.findLeadingChunks(
              OWNER, List.of(first.getValue(), second.getValue(), foreign.getValue()), 3);

      assertThat(results)
          .extracting(DocumentChunk::getContent)
          .containsExactlyInAnyOrder("first-0", "second-0", "first-1");
    }

    private void save(DocumentId documentId, String ownerKey, String content, float[] embedding) {
      repository.saveChunk(
          DocumentChunk.create(
                  ChunkId.generate(), documentId, OwnerKey.parse(ownerKey), content, 0, Map.of())
              .withEmbedding(embedding));
    }

    private void save(DocumentId documentId, String ownerKey, String content, int chunkIndex) {
      repository.saveChunk(
          DocumentChunk.create(
                  ChunkId.generate(),
                  documentId,
                  OwnerKey.parse(ownerKey),
                  content,
                  chunkIndex,
                  Map.of())
              .withEmbedding(new float[] {1f, 0f}));
    }
  }

  @Test
  @DisplayName("should save chunk with embedding via MERGE")
  void shouldSaveChunk() {
    DocumentChunk chunk =
        DocumentChunk.create(
                ChunkId.generate(), DocumentId.generate(), OWNER_KEY, "content", 0, Map.of())
            .withEmbedding(new float[] {0.1f, 0.2f});
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    chunkRepository.saveChunk(chunk);
    verify(jdbcTemplate).update(contains("MERGE INTO"), any(Object[].class));
  }

  @Test
  @DisplayName("should serialize metadata when present")
  void shouldSerializeMetadataWhenPresent() {
    DocumentChunk chunk =
        DocumentChunk.create(
                ChunkId.generate(),
                DocumentId.generate(),
                OWNER_KEY,
                "content",
                0,
                Map.of("key", "value"))
            .withEmbedding(new float[] {0.1f});
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    chunkRepository.saveChunk(chunk);
    verify(jdbcTemplate).update(contains("MERGE INTO"), any(Object[].class));
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
      when(rs.getString("owner_key")).thenReturn("c:owner");
      when(rs.getString("content")).thenReturn(content);
      when(rs.getInt("chunk_index")).thenReturn(1);
      when(rs.getString("embedding")).thenReturn(embedding);
      when(rs.getString("metadata")).thenReturn(metadata);
      when(rs.getString("created_at")).thenReturn(createdAt);

      DocumentChunk result = rowMapper.mapRow(rs, 0);

      assertThat(result.getId().getValue()).isEqualTo(chunkId);
      assertThat(result.getDocumentId().getValue()).isEqualTo(docId);
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
      when(rs.getString("owner_key")).thenReturn("c:owner");
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
      when(rs.getString("owner_key")).thenReturn("c:owner");
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
      when(rs.getString("owner_key")).thenReturn("c:owner");
      when(rs.getString("content")).thenReturn("Test");
      when(rs.getInt("chunk_index")).thenReturn(0);
      when(rs.getString("embedding")).thenReturn("[0.1]");
      when(rs.getString("metadata")).thenReturn("invalid json {");
      when(rs.getString("created_at")).thenReturn("2024-01-01T00:00:00Z");

      DocumentChunk result = rowMapper.mapRow(rs, 0);
      assertThat(result.getMetadata()).isEmpty();
    }
  }

  @Test
  @DisplayName("should find chunks by document ID")
  void shouldFindChunksByDocumentId() {
    DocumentId docId = DocumentId.generate();
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class)))
        .thenReturn(List.of());
    chunkRepository.findChunksByDocumentId(docId);
    verify(jdbcTemplate)
        .query(contains("WHERE document_id = ?"), any(RowMapper.class), eq(docId.getValue()));
  }

  @Test
  @DisplayName("should delete chunks by document ID")
  void shouldDeleteChunksByDocumentId() {
    DocumentId docId = DocumentId.generate();
    when(jdbcTemplate.update(anyString(), any(UUID.class))).thenReturn(1);
    chunkRepository.deleteChunksByDocumentId(docId);
    verify(jdbcTemplate).update(contains("DELETE FROM"), eq(docId.getValue()));
  }
}
