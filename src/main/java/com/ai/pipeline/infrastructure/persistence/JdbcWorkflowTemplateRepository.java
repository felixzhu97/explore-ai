package com.ai.pipeline.infrastructure.persistence;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Documentation. */
@Repository
public class JdbcWorkflowTemplateRepository implements WorkflowTemplateRepository {

  private static final Logger log = LoggerFactory.getLogger(JdbcWorkflowTemplateRepository.class);
  private static final TypeReference<List<String>> AGENT_TYPES_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final RowMapper<SavedWorkflowTemplate> rowMapper;

  /** Documentation. */
  public JdbcWorkflowTemplateRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.rowMapper =
        (rs, rowNum) ->
            SavedWorkflowTemplate.restore(
                WorkflowTemplateId.of(rs.getString("id")),
                rs.getString("owner_key"),
                rs.getString("name"),
                rs.getString("description"),
                parseAgentTypes(rs.getString("agent_types")),
                rs.getString("short_topic"),
                rs.getString("brief_prompt"),
                rs.getString("source_template_id"),
                rs.getBoolean("enabled"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
  }

  @Override
  @Transactional
  public SavedWorkflowTemplate save(SavedWorkflowTemplate template) {
    jdbcTemplate.update(
        """
                MERGE INTO workflow_templates (
                    id, owner_key, name, description, agent_types, short_topic, brief_prompt,
                    source_template_id, enabled, created_at, updated_at
                )
                KEY (id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
        template.getId().value(),
        template.getClientId(),
        template.getName(),
        template.getDescription(),
        serializeAgentTypes(template.getAgentTypes()),
        template.getShortTopic(),
        template.getBriefPrompt(),
        template.getSourceTemplateId(),
        template.isEnabled(),
        Timestamp.from(template.getCreatedAt()),
        Timestamp.from(template.getUpdatedAt()));
    return template;
  }

  @Override
  public Optional<SavedWorkflowTemplate> findByIdAndClientId(
      WorkflowTemplateId id, String clientId) {
    List<SavedWorkflowTemplate> results =
        jdbcTemplate.query(
            """
                SELECT id, owner_key, name, description, agent_types, short_topic, brief_prompt,
                       source_template_id, enabled, created_at, updated_at
                FROM workflow_templates
                WHERE id = ? AND owner_key = ?
                """,
            rowMapper,
            id.value(),
            clientId);
    return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
  }

  @Override
  public List<SavedWorkflowTemplate> findAllByClientId(String clientId) {
    return jdbcTemplate.query(
        """
                SELECT id, owner_key, name, description, agent_types, short_topic, brief_prompt,
                       source_template_id, enabled, created_at, updated_at
                FROM workflow_templates
                WHERE owner_key = ?
                ORDER BY name ASC
                """,
        rowMapper,
        clientId);
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(WorkflowTemplateId id, String clientId) {
    jdbcTemplate.update(
        "DELETE FROM workflow_templates WHERE id = ? AND owner_key = ?", id.value(), clientId);
  }

  @Override
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, WorkflowTemplateId excludeId) {
    Integer count;
    if (excludeId == null) {
      count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM workflow_templates WHERE owner_key = ? AND name = ?",
              Integer.class,
              clientId,
              name);
    } else {
      count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM workflow_templates"
                  + " WHERE owner_key = ? AND name = ? AND id <> ?",
              Integer.class,
              clientId,
              name,
              excludeId.value());
    }
    return count != null && count > 0;
  }

  private String serializeAgentTypes(List<String> agentTypes) {
    try {
      return objectMapper.writeValueAsString(agentTypes);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize agent types", e);
      return "[]";
    }
  }

  private List<String> parseAgentTypes(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, AGENT_TYPES_TYPE);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse agent types JSON", e);
      return List.of();
    }
  }
}
