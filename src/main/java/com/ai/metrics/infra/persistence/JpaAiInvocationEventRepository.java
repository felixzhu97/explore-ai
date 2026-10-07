package com.ai.metrics.infra.persistence;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.ErrorSummary;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.model.TokenUsage;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA insert adapter with JDBC-backed drill-down and retention deletes. */
@Repository
@RequiredArgsConstructor
public class JpaAiInvocationEventRepository implements AiInvocationEventRepository {

  private static final RowMapper<AiInvocationEvent> ROW_MAPPER =
      (rs, rowNum) -> {
        AiCapability capability = AiCapability.parseCapability(rs.getString("capability"));
        String operation = rs.getString("operation");
        Latency latency = Latency.createFromMillis(rs.getLong("latency_ms"));
        OwnerKey owner = OwnerKey.parseKey(rs.getString("owner_key"));
        AiInvocationEvent.Builder event =
            InvocationOutcome.parseOutcome(rs.getString("outcome")) == InvocationOutcome.SUCCESS
                ? AiInvocationEvent.createSucceededEvent(capability, operation, latency, owner)
                : AiInvocationEvent.createFailedEvent(
                    capability,
                    operation,
                    latency,
                    owner,
                    ErrorSummary.createSummary(
                        rs.getString("error_code"), rs.getString("error_message")));
        return event
            .id(UUID.fromString(rs.getString("id")))
            .occurredAt(rs.getObject("occurred_at", Instant.class))
            .provider(rs.getString("provider"))
            .model(rs.getString("model"))
            .sessionId(rs.getString("session_id"))
            .documentId(rs.getString("document_id"))
            .agentType(rs.getString("agent_type"))
            .toolName(rs.getString("tool_name"))
            .tokens(
                new TokenUsage(
                    (Integer) rs.getObject("prompt_tokens"),
                    (Integer) rs.getObject("completion_tokens")))
            .build();
      };

  private final EntityManager entityManager;
  private final JdbcTemplate jdbcTemplate;

  @Override
  public PageResult findDrilldown(DrilldownQuery query) {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> args = new ArrayList<>();

    query
        .capability()
        .ifPresent(
            capability -> {
              where.append(" AND capability = ?");
              args.add(capability.getValue());
            });
    query
        .from()
        .ifPresent(
            from -> {
              where.append(" AND occurred_at >= ?");
              args.add(from);
            });
    query
        .to()
        .ifPresent(
            to -> {
              where.append(" AND occurred_at < ?");
              args.add(to);
            });
    query
        .day()
        .ifPresent(
            day -> {
              LocalDate date = LocalDate.parse(day);
              Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
              Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
              where.append(" AND occurred_at >= ? AND occurred_at < ?");
              args.add(start);
              args.add(end);
            });
    query
        .outcome()
        .ifPresent(
            outcome -> {
              where.append(" AND outcome = ?");
              args.add(outcome.getValue());
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
            "SELECT COUNT(*) FROM ai_invocation_event" + where, Long.class, args.toArray());
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
                SELECT id, occurred_at, capability, operation, outcome, latency_ms,
                       provider, model, session_id, document_id, agent_type, tool_name,
                       prompt_tokens, completion_tokens, error_code, error_message, owner_key
                FROM ai_invocation_event
                """
                + where
                + " ORDER BY occurred_at DESC LIMIT ? OFFSET ?",
            ROW_MAPPER,
            pageArgs.toArray());

    return new PageResult(items, totalCount);
  }

  @Override
  @Transactional
  public void save(AiInvocationEvent event) {
    entityManager.persist(event);
    entityManager.flush();
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
        "DELETE FROM ai_invocation_event WHERE session_id IN (" + placeholders + ")",
        ids.toArray());
  }

  @Override
  @Transactional
  public int deleteOlderThan(Instant cutoff) {
    return jdbcTemplate.update("DELETE FROM ai_invocation_event WHERE occurred_at < ?", cutoff);
  }
}
