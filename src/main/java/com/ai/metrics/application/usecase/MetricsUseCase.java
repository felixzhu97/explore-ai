package com.ai.metrics.application.usecase;

import com.ai.metrics.application.model.DrilldownPage;
import com.ai.metrics.application.model.MetricsDomainSnapshot;
import com.ai.metrics.application.model.MetricsOverview;
import com.ai.metrics.application.model.NamedCount;
import com.ai.metrics.application.model.SeriesPoint;
import com.ai.metrics.application.model.SeriesSnapshot;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Documentation. */
@Service
public class MetricsUseCase {

  private final MetricsQueryRepository queryRepository;
  private final AiInvocationEventRepository eventRepository;
  private final MetricsHealthGateway healthGateway;

  /** Documentation. */
  public MetricsUseCase(
      MetricsQueryRepository queryRepository,
      AiInvocationEventRepository eventRepository,
      MetricsHealthGateway healthGateway) {
    this.queryRepository = queryRepository;
    this.eventRepository = eventRepository;
    this.healthGateway = healthGateway;
  }

  /** Documentation. */
  public MetricsOverview overview(String range) {
    RangeWindow window = parseRange(range);
    Instant activeSince = Instant.now().minus(24, ChronoUnit.HOURS);

    long requests = queryRepository.countInvocations(Optional.empty(), window.from(), window.to());
    long errors = queryRepository.countErrors(Optional.empty(), window.from(), window.to());
    final var latency =
        queryRepository.latencyPercentiles(Optional.empty(), window.from(), window.to());
    final var tokens = queryRepository.tokenTotals(Optional.empty(), window.from(), window.to());
    var chat = queryRepository.chatInventory(activeSince);
    var rag = queryRepository.ragInventory();
    var agents = healthGateway.agentsHealth();
    var mcp = healthGateway.mcpHealth();

    double errorRate = requests == 0 ? 0.0 : (double) errors / requests;
    final double successRate = 1.0 - errorRate;

    final List<NamedCount> byDomain =
        queryRepository.countByDomain(window.from(), window.to()).stream()
            .map(nc -> new NamedCount(nc.name(), nc.count()))
            .toList();

    Map<String, Object> domains = new LinkedHashMap<>();
    domains.put(
        "chat",
        Map.of(
            "sessionCount", chat.sessionCount(),
            "activeSessionCount", chat.activeSessionCount(),
            "messageCount", chat.messageCount(),
            "webSourceReplyCount", chat.webSourceReplyCount()));
    domains.put(
        "rag",
        Map.of(
            "documentCount", rag.documentCount(),
            "documentsByStatus", rag.documentsByStatus(),
            "chunkCount", rag.chunkCount(),
            "totalFileBytes", rag.totalFileBytes()));
    domains.put("agents", agents);
    domains.put("mcp", mcp);
    domains.put("system", healthGateway.systemStatus());

    return new MetricsOverview(
        window.range(),
        requests,
        errors,
        successRate,
        errorRate,
        latency.p50Ms(),
        latency.p95Ms(),
        tokens.promptTokens(),
        tokens.completionTokens(),
        byDomain,
        domains);
  }

  /** Documentation. */
  public MetricsDomainSnapshot domain(String domainRaw, String range) {
    AiDomain domain = AiDomain.require(domainRaw);
    RangeWindow window = parseRange(range);
    Optional<AiDomain> filter = Optional.of(domain);

    long requests = queryRepository.countInvocations(filter, window.from(), window.to());
    long errors = queryRepository.countErrors(filter, window.from(), window.to());
    var latency = queryRepository.latencyPercentiles(filter, window.from(), window.to());
    var tokens = queryRepository.tokenTotals(filter, window.from(), window.to());
    double errorRate = requests == 0 ? 0.0 : (double) errors / requests;

    Map<String, Object> inventory =
        switch (domain) {
          case CHAT -> {
            var chat = queryRepository.chatInventory(Instant.now().minus(24, ChronoUnit.HOURS));
            yield Map.of(
                "sessionCount", chat.sessionCount(),
                "activeSessionCount", chat.activeSessionCount(),
                "messageCount", chat.messageCount(),
                "webSourceReplyCount", chat.webSourceReplyCount());
          }
          case RAG -> {
            var rag = queryRepository.ragInventory();
            yield Map.of(
                "documentCount", rag.documentCount(),
                "documentsByStatus", rag.documentsByStatus(),
                "chunkCount", rag.chunkCount(),
                "totalFileBytes", rag.totalFileBytes());
          }
          case AGENTS -> healthGateway.agentsHealth();
          case TOOLS ->
              Map.of(
                  "topTools",
                  queryRepository.topTools(filter, window.from(), window.to(), 10).stream()
                      .map(nc -> new NamedCount(nc.name(), nc.count()))
                      .toList());
          case VISION, WORKFLOW -> Map.of("requests", requests, "errors", errors);
        };

    return new MetricsDomainSnapshot(
        domain.value(),
        window.range(),
        requests,
        errors,
        errorRate,
        latency.p50Ms(),
        latency.p95Ms(),
        tokens.promptTokens(),
        tokens.completionTokens(),
        inventory,
        series("requests", domain.value(), window.range()).points(),
        series("calls_by_model", domain.value(), window.range()).points());
  }

  /** Documentation. */
  public SeriesSnapshot series(String name, String domainRaw, String range) {
    RangeWindow window = parseRange(range);
    Optional<AiDomain> domain = AiDomain.parse(domainRaw);
    String seriesName = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

    List<SeriesPoint> points =
        switch (seriesName) {
          case "requests" ->
              toPoints(queryRepository.dailyRequests(domain, window.from(), window.to()));
          case "errors" ->
              toPoints(queryRepository.dailyErrors(domain, window.from(), window.to()));
          case "latency_p95" ->
              toPoints(queryRepository.dailyLatencyP95(domain, window.from(), window.to()));
          case "sessions_created" ->
              toPoints(queryRepository.dailySessionsCreated(window.from(), window.to()));
          case "messages_created" ->
              toPoints(queryRepository.dailyMessagesCreated(window.from(), window.to()));
          case "documents_uploaded" ->
              toPoints(queryRepository.dailyDocumentsUploaded(window.from(), window.to()));
          case "documents_by_status" -> {
            var rag = queryRepository.ragInventory();
            yield rag.documentsByStatus().entrySet().stream()
                .map(e -> new SeriesPoint(e.getKey(), e.getValue()))
                .toList();
          }
          case "calls_by_model" ->
              queryRepository.countByModel(domain, window.from(), window.to()).stream()
                  .map(nc -> new SeriesPoint(nc.name(), nc.count()))
                  .toList();
          case "calls_by_agent" ->
              queryRepository.countByAgentType(window.from(), window.to()).stream()
                  .map(nc -> new SeriesPoint(nc.name(), nc.count()))
                  .toList();
          case "tool_top" ->
              queryRepository.topTools(domain, window.from(), window.to(), 10).stream()
                  .map(nc -> new SeriesPoint(nc.name(), nc.count()))
                  .toList();
          case "tokens" -> {
            var tokens = queryRepository.tokenTotals(domain, window.from(), window.to());
            yield List.of(
                new SeriesPoint("prompt", tokens.promptTokens()),
                new SeriesPoint("completion", tokens.completionTokens()));
          }
          default -> throw new IllegalArgumentException("Unknown series name: " + name);
        };

    return new SeriesSnapshot(
        seriesName, domain.map(AiDomain::value).orElse(null), window.range(), points);
  }

  /** Documentation. */
  public DrilldownPage drilldown(
      String domainRaw,
      String from,
      String to,
      String day,
      String outcome,
      String model,
      String agentType,
      String toolName,
      int page,
      int size,
      String range) {
    Optional<Instant> fromInstant = parseInstant(from);
    Optional<Instant> toInstant = parseInstant(to);
    int safeSize = size <= 0 ? 20 : size;
    int safePage = Math.max(0, page);
    if (fromInstant.isEmpty() && toInstant.isEmpty() && (day == null || day.isBlank())) {
      RangeWindow window = parseRange(range == null || range.isBlank() ? "7d" : range);
      fromInstant = Optional.of(window.from());
      toInstant = Optional.of(window.to());
    }

    var result =
        eventRepository.findDrilldown(
            new AiInvocationEventRepository.DrilldownQuery(
                AiDomain.parse(domainRaw),
                fromInstant,
                toInstant,
                Optional.ofNullable(blankToNull(day)),
                Optional.ofNullable(blankToNull(outcome)).map(InvocationOutcome::parse),
                Optional.ofNullable(blankToNull(model)),
                Optional.ofNullable(blankToNull(agentType)),
                Optional.ofNullable(blankToNull(toolName)),
                safePage,
                safeSize));

    return new DrilldownPage(result.items(), result.total(), safePage, safeSize);
  }

  private List<SeriesPoint> toPoints(List<MetricsQueryRepository.TimePoint> points) {
    return points.stream().map(p -> new SeriesPoint(p.day(), p.value())).toList();
  }

  private RangeWindow parseRange(String range) {
    String normalized =
        range == null || range.isBlank() ? "7d" : range.trim().toLowerCase(Locale.ROOT);
    long days =
        switch (normalized) {
          case "30d" -> 30;
          case "7d" -> 7;
          default -> throw new IllegalArgumentException("Unsupported range: " + range);
        };
    Instant to = Instant.now();
    Instant from = to.minus(days, ChronoUnit.DAYS);
    return new RangeWindow(normalized, from, to);
  }

  private Optional<Instant> parseInstant(String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(Instant.parse(raw));
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record RangeWindow(String range, Instant from, Instant to) {}
}
