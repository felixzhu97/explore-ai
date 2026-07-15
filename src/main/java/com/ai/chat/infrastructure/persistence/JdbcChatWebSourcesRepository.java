package com.ai.chat.infrastructure.persistence;

import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.vo.ContentHash;
import com.ai.chat.domain.vo.WebSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcChatWebSourcesRepository implements ChatWebSourcesRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcChatWebSourcesRepository.class);
    private static final TypeReference<List<WebSource>> SOURCES_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcChatWebSourcesRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void save(String conversationId, String assistantContent, String query, List<WebSource> sources) {
        if (conversationId == null || conversationId.isBlank()
                || assistantContent == null || assistantContent.isBlank()
                || sources == null || sources.isEmpty()) {
            return;
        }
        String contentHash = ContentHash.sha256(assistantContent);
        String sourcesJson;
        try {
            sourcesJson = objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize web sources for conversation {}", conversationId, e);
            return;
        }
        String truncatedQuery = query == null ? null : query.substring(0, Math.min(query.length(), 512));
        jdbcTemplate.update(
                """
                MERGE INTO chat_web_sources (conversation_id, content_hash, query, sources_json, created_at)
                KEY (conversation_id, content_hash)
                VALUES (?, ?, ?, ?, ?)
                """,
                conversationId,
                contentHash,
                truncatedQuery,
                sourcesJson,
                Timestamp.from(Instant.now()));
    }

    @Override
    public Map<String, List<WebSource>> findByConversationId(String conversationId) {
        Map<String, List<WebSource>> byHash = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT content_hash, sources_json
                FROM chat_web_sources
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
        jdbcTemplate.update("DELETE FROM chat_web_sources WHERE conversation_id = ?", conversationId);
    }

    private List<WebSource> parseSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<WebSource> parsed = objectMapper.readValue(json, SOURCES_TYPE);
            return parsed == null ? List.of() : List.copyOf(parsed);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse stored web sources JSON", e);
            return List.of();
        }
    }
}
