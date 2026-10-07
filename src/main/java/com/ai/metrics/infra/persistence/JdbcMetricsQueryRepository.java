package com.ai.metrics.infra.persistence;

import com.ai.metrics.domain.model.AiDomain;
import com.ai.metrics.domain.model.InvocationStats;
import com.ai.metrics.domain.model.LatencyStats;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC repository that aggregates invocation, chat, and RAG metrics with SQL queries. */
@Repository
@RequiredArgsConstructor
public class JdbcMetricsQueryRepository implements MetricsQueryRepository {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public InvocationStats countInvocationStats(Optional<AiDomain> domain, Instant from, Instant to) {
    StringBuilder sql =
        new StringBuilder(
            """
                SELECT COUNT(*) AS requests,
                       COALESCE(SUM(CASE WHEN outcome = 'error' THEN 1 ELSE 0 END), 0) AS errors
                FROM ai_invocation_event
                WHERE 1=1
                """);
    List<Object> args = new ArrayList<>();
    appendDomainAndRange(sql, args, domain, from, to, true);
    return jdbcTemplate.query(
        sql.toString(),
        rs -> {
          if (!rs.next()) {
            return new InvocationStats(0L, 0L);
          }
          return new InvocationStats(rs.getLong("requests"), rs.getLong("errors"));
        },
        args.toArray());
  }

  @Override
  public LatencyStats calculateLatencyPercentiles(
      Optional<AiDomain> domain, Instant from, Instant to) {
    StringBuilder sql = new StringBuilder("SELECT latency_ms FROM ai_invocation_event WHERE 1=1");
    List<Object> args = new ArrayList<>();
    appendDomainAndRange(sql, args, domain, from, to, true);
    sql.append(" ORDER BY latency_ms");
    List<Long> latencies =
        jdbcTemplate.query(
            sql.toString(), (rs, rowNum) -> rs.getLong("latency_ms"), args.toArray());
    return LatencyStats.fromSorted(latencies);
  }

  @Override
  public TokenTotals sumTokens(Optional<AiDomain> domain, Instant from, Instant to) {
    StringBuilder sql =
        new StringBuilder(
            """
                SELECT COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
                       COALESCE(SUM(completion_tokens), 0) AS completion_tokens
                FROM ai_invocation_event
                WHERE 1=1
                """);
    List<Object> args = new ArrayList<>();
    appendDomainAndRange(sql, args, domain, from, to, true);
    return jdbcTemplate.query(
        sql.toString(),
        rs -> {
          if (!rs.next()) {
            return new TokenTotals(0L, 0L);
          }
          return new TokenTotals(rs.getLong("prompt_tokens"), rs.getLong("completion_tokens"));
        },
        args.toArray());
  }

  @Override
  public List<NamedCount> countByDomain(Instant from, Instant to) {
    return queryNamedCounts(
        """
                SELECT domain AS name, COUNT(*) AS cnt
                FROM ai_invocation_event
                WHERE occurred_at >= ? AND occurred_at < ?
                GROUP BY domain
                ORDER BY cnt DESC
                """,
        from,
        to);
  }

  @Override
  public List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to) {
    StringBuilder sql =
        new StringBuilder(
            """
                SELECT COALESCE(model, 'unknown') AS name, COUNT(*) AS cnt
                FROM ai_invocation_event
                WHERE 1=1
                """);
    List<Object> args = new ArrayList<>();
    appendDomainAndRange(sql, args, domain, from, to, true);
    sql.append(" GROUP BY COALESCE(model, 'unknown') ORDER BY cnt DESC");
    return jdbcTemplate.query(
        sql.toString(),
        (rs, rowNum) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")),
        args.toArray());
  }

  @Override
  public List<NamedCount> countByAgentType(Instant from, Instant to) {
    return queryNamedCounts(
        """
                SELECT COALESCE(agent_type, 'unknown') AS name, COUNT(*) AS cnt
                FROM ai_invocation_event
                WHERE domain = 'agents' AND occurred_at >= ? AND occurred_at < ?
                GROUP BY COALESCE(agent_type, 'unknown')
                ORDER BY cnt DESC
                """,
        from,
        to);
  }

  @Override
  public List<NamedCount> listTopTools(
      Optional<AiDomain> domain, Instant from, Instant to, int limit) {
    StringBuilder sql =
        new StringBuilder(
            """
                SELECT COALESCE(tool_name, 'unknown') AS name, COUNT(*) AS cnt
                FROM ai_invocation_event
                WHERE tool_name IS NOT NULL
                """);
    List<Object> args = new ArrayList<>();
    appendDomainAndRange(sql, args, domain, from, to, true);
    sql.append(" GROUP BY COALESCE(tool_name, 'unknown') ORDER BY cnt DESC LIMIT ?");
    args.add(Math.max(1, limit));
    return jdbcTemplate.query(
        sql.toString(),
        (rs, rowNum) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")),
        args.toArray());
  }

  @Override
  public List<TimePoint> countDailyRequests(Optional<AiDomain> domain, Instant from, Instant to) {
    return countDailyFromEvents("COUNT(*)", domain, from, to, null);
  }

  @Override
  public List<TimePoint> countDailyErrors(Optional<AiDomain> domain, Instant from, Instant to) {
    return countDailyFromEvents("COUNT(*)", domain, from, to, "outcome = 'error'");
  }

  @Override
  public List<TimePoint> calculateDailyLatencyP95(
      Optional<AiDomain> domain, Instant from, Instant to) {
    StringBuilder sql =
        new StringBuilder(
            """
                SELECT CAST(occurred_at AS DATE) AS bucket_day, latency_ms
                FROM ai_invocation_event
                WHERE 1=1
                """);
    List<Object> args = new ArrayList<>();
    appendDomainAndRange(sql, args, domain, from, to, true);
    sql.append(" ORDER BY bucket_day, latency_ms");
    Map<String, List<Long>> byDay = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql.toString(),
        rs -> {
          String day = rs.getString("bucket_day");
          byDay.computeIfAbsent(day, key -> new ArrayList<>()).add(rs.getLong("latency_ms"));
        },
        args.toArray());
    List<TimePoint> points = new ArrayList<>();
    byDay.forEach(
        (day, values) ->
            points.add(new TimePoint(day, Math.round(LatencyStats.percentile(values, 0.95)))));
    return points;
  }

  @Override
  public List<TimePoint> countDailySessionsCreated(Instant from, Instant to) {
    return jdbcTemplate.query(
        """
                SELECT CAST(created_at AS DATE) AS bucket_day, COUNT(*) AS metric_value
                FROM chat_session
                WHERE created_at >= ? AND created_at < ?
                GROUP BY CAST(created_at AS DATE)
                ORDER BY bucket_day
                """,
        (rs, rowNum) -> new TimePoint(rs.getString("bucket_day"), rs.getLong("metric_value")),
        from,
        to);
  }

  @Override
  public List<TimePoint> countDailyMessagesCreated(Instant from, Instant to) {
    return jdbcTemplate.query(
        """
                SELECT CAST(timestamp AS DATE) AS bucket_day, COUNT(*) AS metric_value
                FROM SPRING_AI_CHAT_MEMORY
                WHERE timestamp >= ? AND timestamp < ?
                GROUP BY CAST(timestamp AS DATE)
                ORDER BY bucket_day
                """,
        (rs, rowNum) -> new TimePoint(rs.getString("bucket_day"), rs.getLong("metric_value")),
        from,
        to);
  }

  @Override
  public List<TimePoint> countDailyDocumentsUploaded(Instant from, Instant to) {
    return jdbcTemplate.query(
        """
                SELECT CAST(created_at AS DATE) AS bucket_day, COUNT(*) AS metric_value
                FROM rag_document
                WHERE created_at >= ? AND created_at < ?
                GROUP BY CAST(created_at AS DATE)
                ORDER BY bucket_day
                """,
        (rs, rowNum) -> new TimePoint(rs.getString("bucket_day"), rs.getLong("metric_value")),
        from,
        to);
  }

  @Override
  public ChatInventory getChatInventory(Instant activeSince) {
    Long sessions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_session", Long.class);
    Long active =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_session WHERE last_activity_at >= ?",
            Long.class,
            activeSince);
    Long messages =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SPRING_AI_CHAT_MEMORY", Long.class);
    Long webSources =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_web_sources", Long.class);
    return new ChatInventory(
        toZeroIfNull(sessions),
        toZeroIfNull(active),
        toZeroIfNull(messages),
        toZeroIfNull(webSources));
  }

  @Override
  public RagInventory getRagInventory() {
    Long documents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rag_document", Long.class);
    Long chunks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM document_chunks", Long.class);
    Long bytes =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(file_size), 0) FROM rag_document", Long.class);
    Map<String, Long> byStatus = new LinkedHashMap<>();
    jdbcTemplate.query(
        "SELECT status, COUNT(*) AS cnt FROM rag_document GROUP BY status",
        rs -> {
          byStatus.put(rs.getString("status"), rs.getLong("cnt"));
        });
    return new RagInventory(
        toZeroIfNull(documents), byStatus, toZeroIfNull(chunks), toZeroIfNull(bytes));
  }

  private List<TimePoint> countDailyFromEvents(
      String valueExpr, Optional<AiDomain> domain, Instant from, Instant to, String extraWhere) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT CAST(occurred_at AS DATE) AS bucket_day, "
                + valueExpr
                + " AS metric_value FROM ai_invocation_event WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (extraWhere != null && !extraWhere.isBlank()) {
      sql.append(" AND ").append(extraWhere);
    }
    appendDomainAndRange(sql, args, domain, from, to, true);
    sql.append(" GROUP BY CAST(occurred_at AS DATE) ORDER BY bucket_day");
    return jdbcTemplate.query(
        sql.toString(),
        (rs, rowNum) -> new TimePoint(rs.getString("bucket_day"), rs.getLong("metric_value")),
        args.toArray());
  }

  private List<NamedCount> queryNamedCounts(String sql, Instant from, Instant to) {
    return jdbcTemplate.query(
        sql, (rs, rowNum) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")), from, to);
  }

  private void appendDomainAndRange(
      StringBuilder sql,
      List<Object> args,
      Optional<AiDomain> domain,
      Instant from,
      Instant to,
      boolean alreadyHasWhere) {
    String joiner = alreadyHasWhere ? " AND " : " WHERE ";
    if (domain.isPresent()) {
      sql.append(joiner).append("domain = ?");
      args.add(domain.get().value());
      joiner = " AND ";
    }
    sql.append(joiner).append("occurred_at >= ? AND occurred_at < ?");
    args.add(from);
    args.add(to);
  }

  private static long toZeroIfNull(Long value) {
    return value == null ? 0L : value;
  }
}
