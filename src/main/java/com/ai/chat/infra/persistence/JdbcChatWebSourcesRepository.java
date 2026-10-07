package com.ai.chat.infra.persistence;

import com.ai.chat.domain.model.ContentHash;
import com.ai.chat.domain.model.WebSource;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JDBC store of web search sources keyed by conversation and assistant reply content hash. */
@Repository
@RequiredArgsConstructor
public class JdbcChatWebSourcesRepository implements ChatWebSourcesRepository {

  private static final TypeReference<List<StoredSource>> SOURCES_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void save(
      String conversationId, String assistantContent, String query, List<WebSource> sources) {
    if (conversationId == null
        || conversationId.isBlank()
        || assistantContent == null
        || assistantContent.isBlank()
        || sources == null
        || sources.isEmpty()) {
      return;
    }
    String contentHash = ContentHash.computeSha256(assistantContent);
    String sourcesJson;
    try {
      sourcesJson = objectMapper.writeValueAsString(sources);
    } catch (JsonProcessingException e) {
      return;
    }
    String truncatedQuery =
        query == null ? null : query.substring(0, Math.min(query.length(), 512));
    jdbcTemplate.update(
        """
                MERGE INTO chat_web_source (
                    conversation_id, content_hash, query, sources_json, created_at)
                KEY (conversation_id, content_hash)
                VALUES (?, ?, ?, ?, ?)
                """,
        conversationId,
        contentHash,
        truncatedQuery,
        sourcesJson,
        Instant.now());
  }

  @Override
  public Map<String, List<WebSource>> findByConversationId(String conversationId) {
    Map<String, List<WebSource>> byHash = new LinkedHashMap<>();
    jdbcTemplate.query(
        """
                SELECT content_hash, sources_json
                FROM chat_web_source
                WHERE conversation_id = ?
                """,
        rs -> {
          String hash = rs.getString("content_hash");
          String json = rs.getString("sources_json");
          byHash.put(hash, parseSources(json));
        },
        conversationId);
    return byHash;
  }

  @Override
  @Transactional
  public void deleteByConversationId(String conversationId) {
    jdbcTemplate.update("DELETE FROM chat_web_source WHERE conversation_id = ?", conversationId);
  }

  private List<WebSource> parseSources(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<StoredSource> parsed = objectMapper.readValue(json, SOURCES_TYPE);
      return parsed == null
          ? List.of()
          : parsed.stream().map(StoredSource::createWebSource).toList();
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }

  private record StoredSource(String title, String url, String snippet, String publishedAt) {
    WebSource createWebSource() {
      return new WebSource(title, url, snippet, publishedAt);
    }
  }
}
