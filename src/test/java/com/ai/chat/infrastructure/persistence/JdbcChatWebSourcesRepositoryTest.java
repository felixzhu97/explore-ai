package com.ai.chat.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.vo.ContentHash;
import com.ai.chat.domain.vo.WebSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcChatWebSourcesRepository")
class JdbcChatWebSourcesRepositoryTest {

  @Mock private JdbcTemplate jdbcTemplate;

  private JdbcChatWebSourcesRepository repository;

  @BeforeEach
  void setUp() {
    repository = new JdbcChatWebSourcesRepository(jdbcTemplate, new ObjectMapper());
  }

  @Test
  @DisplayName("should upsert sources with content hash")
  void shouldUpsertSourcesWithContentHash() {
    List<WebSource> sources = List.of(new WebSource("Title", "https://example.com", "Snippet"));

    repository.save("conv-1", "Assistant reply", "query text", sources);

    verify(jdbcTemplate)
        .update(
            startsWith("MERGE INTO chat_web_sources"),
            eq("conv-1"),
            eq(ContentHash.sha256("Assistant reply")),
            eq("query text"),
            contains("https://example.com"),
            any());
  }

  @Test
  @DisplayName("should skip save when sources empty")
  void shouldSkipSaveWhenSourcesEmpty() {
    repository.save("conv-1", "text", "q", List.of());
    verifyNoInteractions(jdbcTemplate);
  }

  @Test
  @DisplayName("should load sources by conversation")
  void shouldLoadSourcesByConversation() throws Exception {
    String hash = ContentHash.sha256("reply");
    String json =
        new ObjectMapper().writeValueAsString(List.of(new WebSource("T", "https://a.com", "s")));

    doAnswer(
            invocation -> {
              RowCallbackHandler handler = invocation.getArgument(1);
              var rs = mock(java.sql.ResultSet.class);
              when(rs.getString("content_hash")).thenReturn(hash);
              when(rs.getString("sources_json")).thenReturn(json);
              handler.processRow(rs);
              return null;
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowCallbackHandler.class), eq("conv-1"));

    Map<String, List<WebSource>> loaded = repository.findByConversationId("conv-1");

    assertThat(loaded).containsKey(hash);
    assertThat(loaded.get(hash).getFirst().url()).isEqualTo("https://a.com");
  }

  @Test
  @DisplayName("should delete by conversation")
  void shouldDeleteByConversation() {
    repository.deleteByConversationId("conv-1");
    verify(jdbcTemplate).update("DELETE FROM chat_web_sources WHERE conversation_id = ?", "conv-1");
  }
}
