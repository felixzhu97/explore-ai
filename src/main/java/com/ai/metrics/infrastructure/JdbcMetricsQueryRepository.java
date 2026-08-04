package com.ai.metrics.infrastructure;

import com.ai.metrics.domain.MetricsQueryRepository;
import com.ai.metrics.domain.AiDomain;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcMetricsQueryRepository implements MetricsQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMetricsQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countInvocations(Optional<AiDomain> domain, Instant from, Instant to) {
        return countWhere("SELECT COUNT(*) FROM ai_invocation_events", domain, from, to);
    }

    @Override
    public long countErrors(Optional<AiDomain> domain, Instant from, Instant to) {
        return countWhere(
                "SELECT COUNT(*) FROM ai_invocation_events WHERE outcome = 'error'",
                domain,
                from,
                to,
                true);
    }

    @Override
    public LatencyStats latencyPercentiles(Optional<AiDomain> domain, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder(
                "SELECT latency_ms FROM ai_invocation_events WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendDomainAndRange(sql, args, domain, from, to, true);
        sql.append(" ORDER BY latency_ms");
        List<Long> latencies = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> rs.getLong("latency_ms"),
                args.toArray());
        if (latencies.isEmpty()) {
            return new LatencyStats(null, null);
        }
        return new LatencyStats(percentile(latencies, 0.50), percentile(latencies, 0.95));
    }

    @Override
    public TokenTotals tokenTotals(Optional<AiDomain> domain, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
                       COALESCE(SUM(completion_tokens), 0) AS completion_tokens
                FROM ai_invocation_events
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendDomainAndRange(sql, args, domain, from, to, true);
        return jdbcTemplate.query(sql.toString(), rs -> {
            if (!rs.next()) {
                return new TokenTotals(0L, 0L);
            }
            return new TokenTotals(rs.getLong("prompt_tokens"), rs.getLong("completion_tokens"));
        }, args.toArray());
    }

    @Override
    public List<NamedCount> countByDomain(Instant from, Instant to) {
        return namedCounts(
                """
                SELECT domain AS name, COUNT(*) AS cnt
                FROM ai_invocation_events
                WHERE occurred_at >= ? AND occurred_at < ?
                GROUP BY domain
                ORDER BY cnt DESC
                """,
                from,
                to);
    }

    @Override
    public List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(model, 'unknown') AS name, COUNT(*) AS cnt
                FROM ai_invocation_events
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
        return namedCounts(
                """
                SELECT COALESCE(agent_type, 'unknown') AS name, COUNT(*) AS cnt
                FROM ai_invocation_events
                WHERE domain = 'agents' AND occurred_at >= ? AND occurred_at < ?
                GROUP BY COALESCE(agent_type, 'unknown')
                ORDER BY cnt DESC
                """,
                from,
                to);
    }

    @Override
    public List<NamedCount> topTools(Optional<AiDomain> domain, Instant from, Instant to, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(tool_name, 'unknown') AS name, COUNT(*) AS cnt
                FROM ai_invocation_events
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
    public List<TimePoint> dailyRequests(Optional<AiDomain> domain, Instant from, Instant to) {
        return dailyFromEvents("COUNT(*)", domain, from, to, null);
    }

    @Override
    public List<TimePoint> dailyErrors(Optional<AiDomain> domain, Instant from, Instant to) {
        return dailyFromEvents("COUNT(*)", domain, from, to, "outcome = 'error'");
    }

    @Override
    public List<TimePoint> dailyLatencyP95(Optional<AiDomain> domain, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT CAST(occurred_at AS DATE) AS bucket_day, latency_ms
                FROM ai_invocation_events
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendDomainAndRange(sql, args, domain, from, to, true);
        sql.append(" ORDER BY bucket_day, latency_ms");
        Map<String, List<Long>> byDay = new LinkedHashMap<>();
        jdbcTemplate.query(sql.toString(), rs -> {
            String day = rs.getString("bucket_day");
            byDay.computeIfAbsent(day, key -> new ArrayList<>()).add(rs.getLong("latency_ms"));
        }, args.toArray());
        List<TimePoint> points = new ArrayList<>();
        byDay.forEach((day, values) -> points.add(new TimePoint(day, Math.round(percentile(values, 0.95)))));
        return points;
    }

    @Override
    public List<TimePoint> dailySessionsCreated(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT CAST(created_at AS DATE) AS bucket_day, COUNT(*) AS metric_value
                FROM chat_sessions
                WHERE created_at >= ? AND created_at < ?
                GROUP BY CAST(created_at AS DATE)
                ORDER BY bucket_day
                """,
                (rs, rowNum) -> new TimePoint(rs.getString("bucket_day"), rs.getLong("metric_value")),
                Timestamp.from(from),
                Timestamp.from(to));
    }

    @Override
    public List<TimePoint> dailyMessagesCreated(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT CAST(timestamp AS DATE) AS bucket_day, COUNT(*) AS metric_value
                FROM SPRING_AI_CHAT_MEMORY
                WHERE timestamp >= ? AND timestamp < ?
                GROUP BY CAST(timestamp AS DATE)
                ORDER BY bucket_day
                """,
                (rs, rowNum) -> new TimePoint(rs.getString("bucket_day"), rs.getLong("metric_value")),
                Timestamp.from(from),
                Timestamp.from(to));
    }

    @Override
    public List<TimePoint> dailyDocumentsUploaded(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT CAST(created_at AS DATE) AS bucket_day, COUNT(*) AS metric_value
                FROM documents
                WHERE created_at >= ? AND created_at < ?
                GROUP BY CAST(created_at AS DATE)
                ORDER BY bucket_day
                """,
                (rs, rowNum) -> new TimePoint(rs.getString("bucket_day"), rs.getLong("metric_value")),
                Timestamp.from(from),
                Timestamp.from(to));
    }

    @Override
    public ChatInventory chatInventory(Instant activeSince) {
        Long sessions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_sessions", Long.class);
        Long active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_sessions WHERE last_activity_at >= ?",
                Long.class,
                Timestamp.from(activeSince));
        Long messages = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SPRING_AI_CHAT_MEMORY", Long.class);
        Long webSources = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_web_sources", Long.class);
        return new ChatInventory(
                nullToZero(sessions),
                nullToZero(active),
                nullToZero(messages),
                nullToZero(webSources));
    }

    @Override
    public RagInventory ragInventory() {
        Long documents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM documents", Long.class);
        Long chunks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM document_chunks", Long.class);
        Long bytes = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(file_size), 0) FROM documents",
                Long.class);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT status, COUNT(*) AS cnt FROM documents GROUP BY status",
                rs -> {
                    byStatus.put(rs.getString("status"), rs.getLong("cnt"));
                });
        return new RagInventory(nullToZero(documents), byStatus, nullToZero(chunks), nullToZero(bytes));
    }

    private List<TimePoint> dailyFromEvents(
            String valueExpr,
            Optional<AiDomain> domain,
            Instant from,
            Instant to,
            String extraWhere) {
        StringBuilder sql = new StringBuilder(
                "SELECT CAST(occurred_at AS DATE) AS bucket_day, " + valueExpr + " AS metric_value FROM ai_invocation_events WHERE 1=1");
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

    private List<NamedCount> namedCounts(String sql, Instant from, Instant to) {
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")),
                Timestamp.from(from),
                Timestamp.from(to));
    }

    private long countWhere(String baseSql, Optional<AiDomain> domain, Instant from, Instant to) {
        return countWhere(baseSql, domain, from, to, baseSql.toLowerCase().contains("where"));
    }

    private long countWhere(
            String baseSql,
            Optional<AiDomain> domain,
            Instant from,
            Instant to,
            boolean alreadyHasWhere) {
        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> args = new ArrayList<>();
        appendDomainAndRange(sql, args, domain, from, to, alreadyHasWhere);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return nullToZero(count);
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
        args.add(Timestamp.from(from));
        args.add(Timestamp.from(to));
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static double percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        if (sortedValues.size() == 1) {
            return sortedValues.getFirst();
        }
        double rank = percentile * (sortedValues.size() - 1);
        int low = (int) Math.floor(rank);
        int high = (int) Math.ceil(rank);
        if (low == high) {
            return sortedValues.get(low);
        }
        double weight = rank - low;
        return sortedValues.get(low) * (1 - weight) + sortedValues.get(high) * weight;
    }
}
