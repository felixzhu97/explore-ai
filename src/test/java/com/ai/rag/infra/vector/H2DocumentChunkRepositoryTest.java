package com.ai.rag.infra.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.ChunkId;
import com.ai.rag.domain.vo.DocumentId;
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

  private static final Map<String, Object> OWNER_METADATA = Map.of("ownerKey", "c:owner");

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
          "CREATE TABLE document_chunks (id UUID PRIMARY KEY, document_id UUID,"
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

      List<DocumentChunk> results = repository.search(new float[] {1f, 0f}, 5, OWNER, List.of());

      assertThat(results).extracting(DocumentChunk::getContent).containsExactly("high", "low");
    }

    @Test
    @DisplayName("should ignore another owner document when its id is requested")
    void shouldIgnoreAnotherOwnerDocumentWhenItsIdIsRequested() {
      DocumentId own = DocumentId.generate();
      DocumentId foreign = DocumentId.generate();
      save(own, OWNER, "own", new float[] {1f, 0f});
      save(foreign, OTHER_OWNER, "foreign", new float[] {1f, 0f});

      List<DocumentChunk> results =
          repository.search(
              new float[] {1f, 0f}, 5, OWNER, List.of(own.asUuid(), foreign.asUuid()));

      assertThat(results).extracting(DocumentChunk::getContent).containsExactly("own");
    }

    private void save(DocumentId documentId, String ownerKey, String content, float[] embedding) {
      repository.saveChunk(
          DocumentChunk.create(
                  ChunkId.generate(), documentId, content, 0, Map.of("ownerKey", ownerKey))
              .withEmbedding(embedding));
    }
  }

  @Nested
  @DisplayName("SaveChunk")
  class SaveChunk {

    @Test
    @DisplayName("should save chunk with embedding via MERGE")
    void shouldSaveChunk() {
      DocumentChunk chunk =
          DocumentChunk.create(
                  ChunkId.generate(), DocumentId.generate(), "content", 0, OWNER_METADATA)
              .withEmbedding(new float[] {0.1f, 0.2f});
      when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
      chunkRepository.saveChunk(chunk);
      verify(jdbcTemplate).update(contains("MERGE INTO"), any(Object[].class));
    }

    @Test
    @DisplayName("should reject chunk when metadata has no owner key")
    void shouldRejectChunkWhenMetadataHasNoOwnerKey() {
      DocumentChunk chunk =
          DocumentChunk.create(ChunkId.generate(), DocumentId.generate(), "content", 0, Map.of())
              .withEmbedding(new float[] {0.1f});

      assertThatThrownBy(() -> chunkRepository.saveChunk(chunk))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ownerKey");
      verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("should serialize metadata when present")
    void shouldSerializeMetadataWhenPresent() {
      DocumentChunk chunk =
          DocumentChunk.create(
                  ChunkId.generate(),
                  DocumentId.generate(),
                  "content",
                  0,
                  Map.of("key", "value", "ownerKey", "c:owner"))
              .withEmbedding(new float[] {0.1f});
      when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
      chunkRepository.saveChunk(chunk);
      verify(jdbcTemplate).update(contains("MERGE INTO"), any(Object[].class));
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

      assertThat(result.getId().value()).isEqualTo(chunkId.toString());
      assertThat(result.getDocumentId().value()).isEqualTo(docId.toString());
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
      chunkRepository.findChunksByDocumentId(docId);
      verify(jdbcTemplate)
          .query(contains("WHERE document_id = ?"), any(RowMapper.class), eq(docId.value()));
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
      chunkRepository.deleteChunksByDocumentId(docId);
      verify(jdbcTemplate).update(contains("DELETE FROM"), eq(docId.value()));
    }
  }

  @Nested
  @DisplayName("countChunksByDocumentIds")
  class CountChunksByDocumentIds {

    private JdbcTemplate h2;
    private H2DocumentChunkRepository repository;

    @BeforeEach
    void setUpDatabase() {
      h2 =
          new JdbcTemplate(
              new DriverManagerDataSource(
                  "jdbc:h2:mem:chunk-count-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
      h2.execute("CREATE TABLE document_chunks (id UUID PRIMARY KEY, document_id UUID)");
      repository = new H2DocumentChunkRepository(h2, objectMapper);
    }

    @Test
    @DisplayName("should count chunks per document in one grouped query")
    void shouldCountChunksPerDocumentInOneGroupedQuery() {
      DocumentId first = DocumentId.generate();
      DocumentId second = DocumentId.generate();
      DocumentId withoutChunks = DocumentId.generate();
      insertChunks(first, 3);
      insertChunks(second, 1);

      Map<DocumentId, Integer> counts =
          repository.countChunksByDocumentIds(List.of(first, second, withoutChunks));

      assertThat(counts).containsOnly(Map.entry(first, 3), Map.entry(second, 1));
    }

    @Test
    @DisplayName("should return empty map without querying when no ids are given")
    void shouldReturnEmptyMapWithoutQueryingWhenNoIdsAreGiven() {
      assertThat(chunkRepository.countChunksByDocumentIds(List.of())).isEmpty();
      verifyNoInteractions(jdbcTemplate);
    }

    private void insertChunks(DocumentId documentId, int count) {
      for (int i = 0; i < count; i++) {
        h2.update(
            "INSERT INTO document_chunks (id, document_id) VALUES (?, ?)",
            UUID.randomUUID(),
            documentId.asUuid());
      }
    }
  }

  private DocumentChunk createChunk(float[] embedding) {
    return DocumentChunk.create(ChunkId.generate(), DocumentId.generate(), "content", 0, Map.of())
        .withEmbedding(embedding);
  }
}
