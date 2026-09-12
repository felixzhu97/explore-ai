package com.ai.account.infra.persistence;

import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.vo.OwnerKey;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Documentation. */
@Repository
public class JdbcOwnerPartitionRepository implements OwnerPartitionRepository {

  private static final String[] OWNER_TABLES = {
    "chat_session",
    "skill",
    "saved_agent_definition",
    "saved_workflow_template",
    "automation_schedule",
    "automation_run",
    "document",
    "document_chunks",
    "ai_invocation_event"
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
    jdbcTemplate.update("DELETE FROM automation_run WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM automation_schedule WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM document_chunks WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM document WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM ai_invocation_event WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM skill WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM saved_agent_definition WHERE owner_key = ?", key);
    jdbcTemplate.update("DELETE FROM saved_workflow_template WHERE owner_key = ?", key);

    // Chat memory + web sources keyed by conversation id owned by this partition
    jdbcTemplate.update(
        """
                DELETE FROM SPRING_AI_CHAT_MEMORY
                WHERE conversation_id IN (
                    SELECT CAST(id AS VARCHAR) FROM chat_session WHERE owner_key = ?)
                """,
        key);
    jdbcTemplate.update(
        """
                DELETE FROM chat_web_sources
                WHERE conversation_id IN (
                    SELECT CAST(id AS VARCHAR) FROM chat_session WHERE owner_key = ?)
                """,
        key);
    jdbcTemplate.update("DELETE FROM chat_session WHERE owner_key = ?", key);
  }
}
