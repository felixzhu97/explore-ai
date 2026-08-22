package com.ai.metrics.infra.persistence;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA insert adapter with JDBC-backed drill-down and retention deletes. */
@Repository
public class JpaAiInvocationEventRepository implements AiInvocationEventRepository {

  private static final RowMapper<AiInvocationEvent> ROW_MAPPER =
      (rs, rowNum) ->
          AiInvocationEvent.builder()
              .id(java.util.UUID.fromString(rs.getString("id")))
              .occurredAt(rs.getTimestamp("occurred_at").toInstant())
              .domain(AiDomain.require(rs.getString("domain")))
              .operation(rs.getString("operation"))
              .outcome(InvocationOutcome.parse(rs.getString("outcome")))
              .latencyMs(rs.getLong("latency_ms"))
              .provider(rs.getString("provider"))
              .model(rs.getString("model"))
              .sessionId(rs.getString("session_id"))
              .documentId(rs.getString("document_id"))
              .agentType(rs.getString("agent_type"))
              .toolName(rs.getString("tool_name"))
              .promptTokens((Integer) rs.getObject("prompt_tokens"))
              .completionTokens((Integer) rs.getObject("completion_tokens"))
              .errorCode(rs.getString("error_code"))
              .errorMessage(rs.getString("error_message"))
              .build();

  private final SpringDataAiInvocationEventRepository springData;
  private final JdbcTemplate jdbcTemplate;

  /** Documentation. */
  public JpaAiInvocationEventRepository(
      SpringDataAiInvocationEventRepository springData, JdbcTemplate jdbcTemplate) {
    this.springData = springData;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public void save(AiInvocationEvent event) {
    event.assignOwnerKey(resolveOwnerKey(event.getSessionId()));
    springData.saveAndFlush(event);
  }

  @Override
  @Transactional
  public int deleteBySessionIds(Collection<String> sessionIds) {
    if (sessionIds == null || sessionIds.isEmpty()) {
      return 0;
    }
    List<String> ids =
        sessionIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
    if (ids.isEmpty()) {
      return 0;
    }
    String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
    return jdbcTemplate.update(
        "DELETE FROM ai_invocation_events WHERE session_id IN (" + placeholders + ")",
        ids.toArray());
  }

  @Override
  @Transactional
  public int deleteOlderThan(Instant cutoff) {
    return jdbcTemplate.update(
        "DELETE FROM ai_invocation_events WHERE occurred_at < ?", Timestamp.from(cutoff));
  }

  @Override
  public PageResult findDrilldown(DrilldownQuery query) {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> args = new ArrayList<>();

    query
        .domain()
        .ifPresent(
            domain -> {
              where.append(" AND domain = ?");
              args.add(domain.value());
            });
    query
        .from()
        .ifPresent(
            from -> {
              where.append(" AND occurred_at >= ?");
              args.add(Timestamp.from(from));
            });
    query
        .to()
        .ifPresent(
            to -> {
              where.append(" AND occurred_at < ?");
              args.add(Timestamp.from(to));
            });
    query
        .day()
        .ifPresent(
            day -> {
              LocalDate date = LocalDate.parse(day);
              Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
              Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
              where.append(" AND occurred_at >= ? AND occurred_at < ?");
              args.add(Timestamp.from(start));
              args.add(Timestamp.from(end));
            });
    query
        .outcome()
        .ifPresent(
            outcome -> {
              where.append(" AND outcome = ?");
              args.add(outcome.value());
            });
    query
        .model()
        .ifPresent(
            model -> {
              where.append(" AND model = ?");
              args.add(model);
            });
    query
        .agentType()
        .ifPresent(
            agentType -> {
              where.append(" AND agent_type = ?");
              args.add(agentType);
            });
    query
        .toolName()
        .ifPresent(
            toolName -> {
              where.append(" AND tool_name = ?");
              args.add(toolName);
            });

    Long total =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ai_invocation_events" + where, Long.class, args.toArray());
    long totalCount = total == null ? 0L : total;

    int size = Math.max(1, Math.min(query.size(), 100));
    int page = Math.max(0, query.page());
    int offset = page * size;

    List<Object> pageArgs = new ArrayList<>(args);
    pageArgs.add(size);
    pageArgs.add(offset);

    List<AiInvocationEvent> items =
        jdbcTemplate.query(
            """
                SELECT id, occurred_at, domain, operation, outcome, latency_ms,
                       provider, model, session_id, document_id, agent_type, tool_name,
                       prompt_tokens, completion_tokens, error_code, error_message
                FROM ai_invocation_events
                """
                + where
                + " ORDER BY occurred_at DESC LIMIT ? OFFSET ?",
            ROW_MAPPER,
            pageArgs.toArray());

    return new PageResult(items, totalCount);
  }

  private String resolveOwnerKey(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return com.ai.common.domain.vo.OwnerKey.LEGACY_ORPHAN.value();
    }
    List<String> keys =
        jdbcTemplate.query(
            "SELECT owner_key FROM chat_sessions WHERE CAST(id AS VARCHAR) = ?",
            (rs, rowNum) -> rs.getString(1),
            sessionId.trim());
    if (!keys.isEmpty() && keys.getFirst() != null && !keys.getFirst().isBlank()) {
      return keys.getFirst();
    }
    return com.ai.common.domain.vo.OwnerKey.LEGACY_ORPHAN.value();
  }
}
