package com.ai.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.ai.account.domain.event.GuestLinkedToAccountEvent;
import com.ai.automation.service.AutomationService;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.chat.infra.persistence.JdbcChatWebSourcesRepository;
import com.ai.chat.service.ChatSessionEraser;
import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.infra.persistence.JpaAiInvocationEventRepository;
import com.ai.metrics.service.InvocationEventEraser;
import com.ai.pipeline.service.CustomAgentService;
import com.ai.pipeline.service.PipelineTemplateService;
import com.ai.rag.domain.repository.DocumentReader;
import com.ai.rag.domain.repository.DocumentTransformer;
import com.ai.rag.domain.repository.DocumentWriter;
import com.ai.rag.infra.vector.H2DocumentChunkRepository;
import com.ai.rag.service.DocumentUploadService;
import com.ai.skill.service.SkillService;
import com.ai.testsupport.AbstractDataJpaTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Owner partition events")
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.datasource.url=jdbc:h2:mem:owner-partition-events;DB_CLOSE_DELAY=-1"
    })
@Import({
  OwnerErasureService.class,
  ChatSessionEraser.class,
  JdbcChatWebSourcesRepository.class,
  SkillService.class,
  PipelineTemplateService.class,
  CustomAgentService.class,
  AutomationService.class,
  DocumentUploadService.class,
  H2DocumentChunkRepository.class,
  InvocationEventEraser.class,
  JpaAiInvocationEventRepository.class,
  OwnerPartitionEventsTest.Config.class
})
class OwnerPartitionEventsTest extends AbstractDataJpaTest {

  private static final OwnerKey GUEST = OwnerKey.parseKey("c:guest-a");
  private static final OwnerKey ACCOUNT = OwnerKey.parseKey("u:account-a");
  private static final OwnerKey OTHER = OwnerKey.parseKey("c:guest-b");
  @MockitoBean private ConversationMemoryRepository conversationMemoryRepository;
  @MockitoBean private DocumentReader documentReader;
  @MockitoBean private DocumentTransformer documentTransformer;
  @MockitoBean private DocumentWriter documentWriter;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private OwnerErasureService ownerErasureService;
  @Autowired private ApplicationEventPublisher eventPublisher;

  private String guestSessionId;
  private List<String> ownerTables;

  @TestConfiguration
  static class Config {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @BeforeEach
  void seedOwners() {
    ownerTables =
        jdbcTemplate.queryForList(
            "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.COLUMNS"
                + " WHERE TABLE_SCHEMA = 'PUBLIC' AND COLUMN_NAME = 'OWNER_KEY'",
            String.class);
    assertThat(ownerTables).as("seed every owner table below").hasSize(9);
    guestSessionId = seedOwner(GUEST);
    seedOwner(OTHER);
  }

  @Test
  @DisplayName("should leave no owner rows behind when owner erasure is requested")
  void shouldLeaveNoOwnerRowsBehindWhenOwnerErasureIsRequested() {
    ownerErasureService.eraseAllForOwner(GUEST);
    flushAndClear();

    for (String table : ownerTables) {
      assertThat(countRows(table, GUEST)).as(table).isZero();
      assertThat(countRows(table, OTHER)).as(table).isEqualTo(1);
    }
    assertThat(countWebSources(guestSessionId)).isZero();
    verify(conversationMemoryRepository).clearMessages(guestSessionId);
  }

  @Test
  @DisplayName("should move every guest row to the account when a guest is linked")
  void shouldMoveEveryGuestRowToTheAccountWhenAGuestIsLinked() {
    eventPublisher.publishEvent(new GuestLinkedToAccountEvent(GUEST, ACCOUNT));
    flushAndClear();

    for (String table : ownerTables) {
      assertThat(countRows(table, GUEST)).as(table).isZero();
      assertThat(countRows(table, ACCOUNT)).as(table).isEqualTo(1);
      assertThat(countRows(table, OTHER)).as(table).isEqualTo(1);
    }
    assertThat(countWebSources(guestSessionId)).isEqualTo(1);
  }

  private String seedOwner(OwnerKey owner) {
    String key = owner.getValue();
    Instant now = Instant.now();
    UUID sessionId = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID scheduleId = UUID.randomUUID();
    UUID documentId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO chat_session (id, owner_key, title, version, created_at, updated_at,"
            + " last_activity_at) VALUES (?, ?, 'Chat', 0, ?, ?, ?)",
        sessionId,
        key,
        now,
        now,
        now);
    jdbcTemplate.update(
        "INSERT INTO chat_web_source (conversation_id, content_hash, sources_json, created_at)"
            + " VALUES (?, 'hash', '[]', ?)",
        sessionId.toString(),
        now);
    jdbcTemplate.update(
        "INSERT INTO skill (id, owner_key, name, instructions, enabled, version, created_at,"
            + " updated_at) VALUES (?, ?, 'Skill', 'Be brief', TRUE, 0, ?, ?)",
        UUID.randomUUID(),
        key,
        now,
        now);
    jdbcTemplate.update(
        "INSERT INTO custom_agent (id, owner_key, agent_type, name, system_prompt, tools,"
            + " enabled, version, created_at, updated_at)"
            + " VALUES (?, ?, 'custom-writer', 'Writer', 'Write', '[]', TRUE, 0, ?, ?)",
        UUID.randomUUID(),
        key,
        now,
        now);
    jdbcTemplate.update(
        "INSERT INTO pipeline_template (id, owner_key, name, agent_types, brief, enabled,"
            + " version, created_at, updated_at)"
            + " VALUES (?, ?, 'Pipeline', '[]', 'Brief', TRUE, 0, ?, ?)",
        templateId,
        key,
        now,
        now);
    jdbcTemplate.update(
        "INSERT INTO automation_schedule (id, owner_key, name, schedule_kind, timezone, enabled,"
            + " action_type, pipeline_template_id, recipient_email, brief, next_run_at, version,"
            + " created_at, updated_at) VALUES (?, ?, 'Daily', 'CRON', 'UTC', TRUE,"
            + " 'RUN_PIPELINE', ?, 'owner@example.com', 'Brief', ?, 0, ?, ?)",
        scheduleId,
        key,
        templateId,
        now,
        now,
        now);
    jdbcTemplate.update(
        "INSERT INTO automation_run (id, schedule_id, owner_key, started_at, created_at, status,"
            + " email_status) VALUES (?, ?, ?, ?, ?, 'SUCCEEDED', 'SENT')",
        UUID.randomUUID(),
        scheduleId,
        key,
        now,
        now);
    jdbcTemplate.update(
        "INSERT INTO rag_document (id, owner_key, title, status, chunk_count, version,"
            + " created_at, updated_at) VALUES (?, ?, 'Guide', 'READY', 1, 0, ?, ?)",
        documentId,
        key,
        now,
        now);
    jdbcTemplate.update(
        "INSERT INTO document_chunk (id, document_id, owner_key, content, chunk_index,"
            + " created_at) VALUES (?, ?, ?, 'Text', 0, ?)",
        UUID.randomUUID(),
        documentId,
        key,
        now);
    jdbcTemplate.update(
        "INSERT INTO ai_invocation_event (id, owner_key, occurred_at, created_at, capability,"
            + " operation, outcome, latency_ms, session_id)"
            + " VALUES (?, ?, ?, ?, 'CHAT', 'chat', 'SUCCESS', 10, ?)",
        UUID.randomUUID(),
        key,
        now,
        now,
        sessionId.toString());
    return sessionId.toString();
  }

  private int countRows(String table, OwnerKey owner) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE owner_key = ?", Integer.class, owner.getValue());
  }

  private int countWebSources(String sessionId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM chat_web_source WHERE conversation_id = ?", Integer.class, sessionId);
  }
}
