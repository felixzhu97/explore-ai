package com.ai.metrics.service;

import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.domain.vo.InvocationStats;
import com.ai.metrics.domain.vo.MetricsWindow;
import com.ai.metrics.service.model.DomainInventory;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsDomainSnapshot;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.NamedCount;
import com.ai.metrics.service.model.OverviewDomains;
import com.ai.metrics.service.model.SeriesPoint;
import com.ai.metrics.service.model.SeriesSnapshot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Builds metrics overviews, per-domain snapshots, daily series, and event drilldowns. */
@Service
@RequiredArgsConstructor
public class MetricsService {

  private final MetricsQueryRepository queryRepository;
  private final AiInvocationEventRepository eventRepository;
  private final MetricsHealthGateway healthGateway;

  /** Returns cross-domain request, error, latency, token, and inventory totals for the range. */
  public MetricsOverview getOverview(String range) {
    RangeWindow window = RangeWindow.endingNow(range);
    Instant activeSince = window.to().minus(24, ChronoUnit.HOURS);

    InvocationStats stats =
        queryRepository.countInvocationStats(Optional.empty(), window.from(), window.to());
    final var latency =
        queryRepository.calculateLatencyPercentiles(Optional.empty(), window.from(), window.to());
    final var tokens = queryRepository.sumTokens(Optional.empty(), window.from(), window.to());
    var chat = queryRepository.getChatInventory(activeSince);
    var rag = queryRepository.getRagInventory();
    var agents = healthGateway.checkAgentsHealth();
    var mcp = healthGateway.checkMcpHealth();

    final List<NamedCount> byDomain =
        queryRepository.countByDomain(window.from(), window.to()).stream()
            .map(nc -> new NamedCount(nc.name(), nc.count()))
            .toList();

    OverviewDomains domains =
        new OverviewDomains(chat, rag, agents, mcp, healthGateway.getSystemStatus());

    return new MetricsOverview(
        window.range(),
        stats.requests(),
        stats.errors(),
        stats.successRate(),
        stats.errorRate(),
        latency.p50Ms(),
        latency.p95Ms(),
        tokens.promptTokens(),
        tokens.completionTokens(),
        byDomain,
        domains);
  }

  /** Returns the named chart series, optionally filtered by domain, over the given range. */
  public SeriesSnapshot getSeries(String name, String domainRaw, String range) {
    RangeWindow window = RangeWindow.endingNow(range);
    Optional<AiDomain> domain = AiDomain.parse(domainRaw);
    String seriesName = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

    List<SeriesPoint> points =
        switch (seriesName) {
          case "requests" ->
              toPoints(queryRepository.countDailyRequests(domain, window.from(), window.to()));
          case "errors" ->
              toPoints(queryRepository.countDailyErrors(domain, window.from(), window.to()));
          case "latency_p95" ->
              toPoints(
                  queryRepository.calculateDailyLatencyP95(domain, window.from(), window.to()));
          case "sessions_created" ->
              toPoints(queryRepository.countDailySessionsCreated(window.from(), window.to()));
          case "messages_created" ->
              toPoints(queryRepository.countDailyMessagesCreated(window.from(), window.to()));
          case "documents_uploaded" ->
              toPoints(queryRepository.countDailyDocumentsUploaded(window.from(), window.to()));
          case "documents_by_status" -> {
            var rag = queryRepository.getRagInventory();
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
              queryRepository.listTopTools(domain, window.from(), window.to(), 10).stream()
                  .map(nc -> new SeriesPoint(nc.name(), nc.count()))
                  .toList();
          case "tokens" -> {
            var tokens = queryRepository.sumTokens(domain, window.from(), window.to());
            yield List.of(
                new SeriesPoint("prompt", tokens.promptTokens()),
                new SeriesPoint("completion", tokens.completionTokens()));
          }
          default -> throw new IllegalArgumentException("Unknown series name: " + name);
        };

    return new SeriesSnapshot(
        seriesName, domain.map(AiDomain::value).orElse(null), window.range(), points);
  }

  /** Returns a page of invocation events matching the filters, defaulting to the last 7 days. */
  public DrilldownPage getDrilldown(
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
      RangeWindow window = RangeWindow.endingNow(range);
      fromInstant = Optional.of(window.from());
      toInstant = Optional.of(window.to());
    }

    var result =
        eventRepository.findDrilldown(
            new AiInvocationEventRepository.DrilldownQuery(
                AiDomain.parse(domainRaw),
                fromInstant,
                toInstant,
                Optional.ofNullable(toNullIfBlank(day)),
                Optional.ofNullable(toNullIfBlank(outcome)).map(InvocationOutcome::parse),
                Optional.ofNullable(toNullIfBlank(model)),
                Optional.ofNullable(toNullIfBlank(agentType)),
                Optional.ofNullable(toNullIfBlank(toolName)),
                safePage,
                safeSize));

    return new DrilldownPage(result.items(), result.total(), safePage, safeSize);
  }

  /** Returns request stats, domain-specific inventory, and trend series for one AI domain. */
  public MetricsDomainSnapshot getDomain(String domainRaw, String range) {
    AiDomain domain = AiDomain.require(domainRaw);
    RangeWindow window = RangeWindow.endingNow(range);
    Optional<AiDomain> filter = Optional.of(domain);

    InvocationStats stats =
        queryRepository.countInvocationStats(filter, window.from(), window.to());
    var latency = queryRepository.calculateLatencyPercentiles(filter, window.from(), window.to());
    var tokens = queryRepository.sumTokens(filter, window.from(), window.to());

    DomainInventory inventory =
        switch (domain) {
          case CHAT ->
              new DomainInventory.Chat(
                  queryRepository.getChatInventory(window.to().minus(24, ChronoUnit.HOURS)));
          case RAG -> new DomainInventory.Rag(queryRepository.getRagInventory());
          case AGENTS -> new DomainInventory.Agents(healthGateway.checkAgentsHealth());
          case TOOLS ->
              new DomainInventory.Tools(
                  queryRepository.listTopTools(filter, window.from(), window.to(), 10).stream()
                      .map(nc -> new NamedCount(nc.name(), nc.count()))
                      .toList());
          case VISION, WORKFLOW -> new DomainInventory.Requests(stats.requests(), stats.errors());
        };

    return new MetricsDomainSnapshot(
        domain.value(),
        window.range(),
        stats.requests(),
        stats.errors(),
        stats.errorRate(),
        latency.p50Ms(),
        latency.p95Ms(),
        tokens.promptTokens(),
        tokens.completionTokens(),
        inventory,
        getSeries("requests", domain.value(), window.range()).points(),
        getSeries("calls_by_model", domain.value(), window.range()).points());
  }

  private List<SeriesPoint> toPoints(List<MetricsQueryRepository.TimePoint> points) {
    return points.stream().map(p -> new SeriesPoint(p.day(), p.value())).toList();
  }

  private Optional<Instant> parseInstant(String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(Instant.parse(raw));
  }

  private String toNullIfBlank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record RangeWindow(String range, Instant from, Instant to) {
    static RangeWindow endingNow(String range) {
      MetricsWindow window = MetricsWindow.parse(range);
      Instant now = Instant.now();
      return new RangeWindow(window.range(), window.from(now), now);
    }
  }
}
