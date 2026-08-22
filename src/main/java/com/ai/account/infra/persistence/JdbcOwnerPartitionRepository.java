package com.ai.account.infra.persistence;

import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.vo.OwnerKey;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Documentation. */
@Repository
public class JdbcOwnerPartitionRepository implements OwnerPartitionRepository {

  private static final String[] OWNER_TABLES = {
    "chat_sessions",
    "skills",
    "pipeline_agents",
    "workflow_templates",
    "automation_schedules",
    "automation_runs",
    "documents",
    "document_chunks",
    "ai_invocation_events"
  };

  private final JdbcTemplate jdbcTemplate;

  /** Documentation. */
  public JdbcOwnerPartitionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void reassignOwner(OwnerKey from, OwnerKey to) {
    for (String table : OWNER_TABLES) {
      jdbcTemplate.update(
          "UPDATE " + table + " SET owner_key = ? WHERE owner_key = ?", to.value(), from.value());
    }
  }

  @Override
  public void deleteAllForOwner(OwnerKey owner) {
    String key = owner.value();
    // Child / dependent tables first where FK-like ordering matters
    jdbcTemplate.update("DELETE FROM automation_runs WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM automation_schedules WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM document_chunks WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM documents WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM ai_invocation_events WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM skills WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM pipeline_agents WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM workflow_templates WHERE owner_key = ?", key);

    // Chat memory + web sources keyed by conversation id owned by this partition
    jdbcTemplate.update(
        """
                DELETE FROM SPRING_AI_CHAT_MEMORY
                WHERE conversation_id IN (
                    SELECT CAST(id AS VARCHAR) FROM chat_sessions WHERE owner_key = ?)
                """,
        key);
    jdbcTemplate.update(
        """
                DELETE FROM chat_web_sources
                WHERE conversation_id IN (
                    SELECT CAST(id AS VARCHAR) FROM chat_sessions WHERE owner_key = ?)
                """,
        key);
    jdbcTemplate.update("DELETE FROM chat_sessions WHERE owner_key = ?", key);
  }
}
