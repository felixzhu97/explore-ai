package com.ai.pipeline.infrastructure.persistence;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import com.ai.pipeline.domain.vo.SavedAgentId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcSavedAgentRepository implements SavedAgentRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcSavedAgentRepository.class);
    private static final TypeReference<List<String>> TOOL_KEYS_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<SavedAgentDefinition> rowMapper;

    public JdbcSavedAgentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rowMapper = (rs, rowNum) -> SavedAgentDefinition.restore(
                SavedAgentId.of(rs.getString("id")),
                rs.getString("owner_key"),
                rs.getString("type_key"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("system_prompt"),
                parseToolKeys(rs.getString("tool_keys")),
                rs.getBoolean("enabled"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    @Override
    @Transactional
    public SavedAgentDefinition save(SavedAgentDefinition agent) {
        jdbcTemplate.update(
                """
                MERGE INTO pipeline_agents (
                    id, owner_key, type_key, name, description, system_prompt, tool_keys,
                    enabled, created_at, updated_at
                )
                KEY (id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                agent.getId().value(),
                agent.getClientId(),
                agent.getTypeKey(),
                agent.getName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                serializeToolKeys(agent.getToolKeys()),
                agent.isEnabled(),
                Timestamp.from(agent.getCreatedAt()),
                Timestamp.from(agent.getUpdatedAt()));
        return agent;
    }

    @Override
    public Optional<SavedAgentDefinition> findByIdAndClientId(SavedAgentId id, String clientId) {
        List<SavedAgentDefinition> results = jdbcTemplate.query(
                """
                SELECT id, owner_key, type_key, name, description, system_prompt, tool_keys,
                       enabled, created_at, updated_at
                FROM pipeline_agents
                WHERE id = ? AND owner_key = ?
                """,
                rowMapper,
                id.value(),
                clientId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<SavedAgentDefinition> findAllByClientId(String clientId) {
        return jdbcTemplate.query(
                """
                SELECT id, owner_key, type_key, name, description, system_prompt, tool_keys,
                       enabled, created_at, updated_at
                FROM pipeline_agents
                WHERE owner_key = ?
                ORDER BY name ASC
                """,
                rowMapper,
                clientId);
    }

    @Override
    public List<SavedAgentDefinition> findEnabledByClientId(String clientId) {
        return jdbcTemplate.query(
                """
                SELECT id, owner_key, type_key, name, description, system_prompt, tool_keys,
                       enabled, created_at, updated_at
                FROM pipeline_agents
                WHERE owner_key = ? AND enabled = TRUE
                ORDER BY name ASC
                """,
                rowMapper,
                clientId);
    }

    @Override
    public void deleteByIdAndClientId(SavedAgentId id, String clientId) {
        jdbcTemplate.update(
                "DELETE FROM pipeline_agents WHERE id = ? AND owner_key = ?",
                id.value(),
                clientId);
    }

    @Override
    public boolean existsByClientIdAndTypeKeyIgnoringId(
            String clientId, String typeKey, SavedAgentId excludeId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM pipeline_agents
                WHERE owner_key = ? AND type_key = ? AND id <> ?
                """,
                Integer.class,
                clientId,
                typeKey,
                excludeId == null ? "" : excludeId.value());
        return count != null && count > 0;
    }

    private List<String> parseToolKeys(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, TOOL_KEYS_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tool_keys JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String serializeToolKeys(List<String> toolKeys) {
        try {
            return objectMapper.writeValueAsString(toolKeys == null ? List.of() : toolKeys);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tool_keys", e);
        }
    }
}
