package com.ai.skill.infra.persistence;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Documentation. */
@Repository
public class JdbcSkillRepository implements SkillRepository {

  private static final Logger log = LoggerFactory.getLogger(JdbcSkillRepository.class);
  private static final TypeReference<List<String>> ALLOWED_TOOLS_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final RowMapper<Skill> rowMapper;

  /** Documentation. */
  public JdbcSkillRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.rowMapper =
        (rs, rowNum) ->
            Skill.restore(
                SkillId.of(rs.getString("id")),
                rs.getString("owner_key"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("instructions"),
                parseAllowedTools(rs.getString("allowed_tools")),
                rs.getBoolean("enabled"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
  }

  @Override
  @Transactional
  public Skill save(Skill skill) {
    jdbcTemplate.update(
        """
                MERGE INTO skills (
                    id, owner_key, name, description, instructions,
                    allowed_tools, enabled, created_at, updated_at
                )
                KEY (id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
        skill.getId().value(),
        skill.getClientId(),
        skill.getName(),
        skill.getDescription(),
        skill.getInstructions(),
        serializeAllowedTools(skill.getAllowedTools()),
        skill.isEnabled(),
        Timestamp.from(skill.getCreatedAt()),
        Timestamp.from(skill.getUpdatedAt()));
    return skill;
  }

  @Override
  public Optional<Skill> findByIdAndClientId(SkillId id, String clientId) {
    List<Skill> results =
        jdbcTemplate.query(
            """
                SELECT id, owner_key, name, description, instructions,
                    allowed_tools, enabled, created_at, updated_at
                FROM skills
                WHERE id = ? AND owner_key = ?
                """,
            rowMapper,
            id.value(),
            clientId);
    return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
  }

  @Override
  public List<Skill> findAllByClientId(String clientId) {
    return jdbcTemplate.query(
        """
                SELECT id, owner_key, name, description, instructions,
                    allowed_tools, enabled, created_at, updated_at
                FROM skills
                WHERE owner_key = ?
                ORDER BY name ASC
                """,
        rowMapper,
        clientId);
  }

  @Override
  public List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
    List<Object> params = new ArrayList<>();
    params.add(clientId);
    ids.forEach(id -> params.add(id.value()));
    return jdbcTemplate.query(
        """
                SELECT id, owner_key, name, description, instructions,
                    allowed_tools, enabled, created_at, updated_at
                FROM skills
                WHERE owner_key = ? AND enabled = TRUE AND id IN (%s)
                ORDER BY name ASC
                """
            .formatted(placeholders),
        rowMapper,
        params.toArray());
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(SkillId id, String clientId) {
    jdbcTemplate.update("DELETE FROM skills WHERE id = ? AND owner_key = ?", id.value(), clientId);
  }

  @Override
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, SkillId excludeId) {
    Integer count;
    if (excludeId == null) {
      count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM skills WHERE owner_key = ? AND name = ?",
              Integer.class,
              clientId,
              name);
    } else {
      count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM skills WHERE owner_key = ? AND name = ? AND id <> ?",
              Integer.class,
              clientId,
              name,
              excludeId.value());
    }
    return count != null && count > 0;
  }

  private String serializeAllowedTools(List<String> allowedTools) {
    if (allowedTools == null || allowedTools.isEmpty()) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(allowedTools);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize allowed tools", e);
      return "[]";
    }
  }

  private List<String> parseAllowedTools(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, ALLOWED_TOOLS_TYPE);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse allowed tools JSON", e);
      return List.of();
    }
  }
}
