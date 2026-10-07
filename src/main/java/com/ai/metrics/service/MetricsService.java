package com.ai.metrics.service;

import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.model.InvocationStats;
import com.ai.metrics.domain.model.MetricsWindow;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.service.model.CapabilityInventory;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsCapabilitySnapshot;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.NamedCount;
import com.ai.metrics.service.model.OverviewCapabilities;
import com.ai.metrics.service.model.SeriesPoint;
import com.ai.metrics.service.model.SeriesSnapshot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Builds metrics overviews, per-capability snapshots, daily series, and event drilldowns. */
@Service
@RequiredArgsConstructor
public class MetricsService {

  private final MetricsQueryRepository queryRepository;
  private final AiInvocationEventRepository eventRepository;
  private final MetricsHealthGateway healthGateway;

  /**
   * Returns cross-capability request, error, latency, token, and inventory totals for the range.
   */
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

    final List<NamedCount> byCapability =
        queryRepository.countByCapability(window.from(), window.to()).stream()
            .map(nc -> new NamedCount(nc.name(), nc.count()))
            .toList();

    OverviewCapabilities capabilities =
        new OverviewCapabilities(chat, rag, agents, mcp, healthGateway.getSystemStatus());

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
        byCapability,
        capabilities);
  }

  /** Returns the named chart series, optionally filtered by capability, over the given range. */
  public SeriesSnapshot getSeries(String name, String capabilityRaw, String range) {
    RangeWindow window = RangeWindow.endingNow(range);
    Optional<AiCapability> capability = AiCapability.parse(capabilityRaw);
    String seriesName = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

    List<SeriesPoint> points =
        switch (seriesName) {
          case "requests" ->
              toPoints(queryRepository.countDailyRequests(capability, window.from(), window.to()));
          case "errors" ->
              toPoints(queryRepository.countDailyErrors(capability, window.from(), window.to()));
          case "latency_p95" ->
              toPoints(
                  queryRepository.calculateDailyLatencyP95(capability, window.from(), window.to()));
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
              queryRepository.countByModel(capability, window.from(), window.to()).stream()
                  .map(nc -> new SeriesPoint(nc.name(), nc.count()))
                  .toList();
          case "calls_by_agent" ->
              queryRepository.countByAgentType(window.from(), window.to()).stream()
                  .map(nc -> new SeriesPoint(nc.name(), nc.count()))
                  .toList();
          case "tool_top" ->
              queryRepository.listTopTools(capability, window.from(), window.to(), 10).stream()
                  .map(nc -> new SeriesPoint(nc.name(), nc.count()))
                  .toList();
          case "tokens" -> {
            var tokens = queryRepository.sumTokens(capability, window.from(), window.to());
            yield List.of(
                new SeriesPoint("prompt", tokens.promptTokens()),
                new SeriesPoint("completion", tokens.completionTokens()));
          }
          default -> throw new IllegalArgumentException("Unknown series name: " + name);
        };

    return new SeriesSnapshot(
        seriesName, capability.map(AiCapability::value).orElse(null), window.range(), points);
  }

  /** Returns a page of invocation events matching the filters, defaulting to the last 7 days. */
  public DrilldownPage getDrilldown(
      String capabilityRaw,
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
                AiCapability.parse(capabilityRaw),
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

  /**
   * Returns request stats, capability-specific inventory, and trend series for one AI capability.
   */
  public MetricsCapabilitySnapshot getCapability(String capabilityRaw, String range) {
    AiCapability capability = AiCapability.require(capabilityRaw);
    RangeWindow window = RangeWindow.endingNow(range);
    Optional<AiCapability> filter = Optional.of(capability);

    InvocationStats stats =
        queryRepository.countInvocationStats(filter, window.from(), window.to());
    var latency = queryRepository.calculateLatencyPercentiles(filter, window.from(), window.to());
    var tokens = queryRepository.sumTokens(filter, window.from(), window.to());

    CapabilityInventory inventory =
        switch (capability) {
          case CHAT ->
              new CapabilityInventory.Chat(
                  queryRepository.getChatInventory(window.to().minus(24, ChronoUnit.HOURS)));
          case RAG -> new CapabilityInventory.Rag(queryRepository.getRagInventory());
          case AGENTS -> new CapabilityInventory.Agents(healthGateway.checkAgentsHealth());
          case TOOLS ->
              new CapabilityInventory.Tools(
                  queryRepository.listTopTools(filter, window.from(), window.to(), 10).stream()
                      .map(nc -> new NamedCount(nc.name(), nc.count()))
                      .toList());
          case VISION, WORKFLOW ->
              new CapabilityInventory.Requests(stats.requests(), stats.errors());
        };

    return new MetricsCapabilitySnapshot(
        capability.value(),
        window.range(),
        stats.requests(),
        stats.errors(),
        stats.errorRate(),
        latency.p50Ms(),
        latency.p95Ms(),
        tokens.promptTokens(),
        tokens.completionTokens(),
        inventory,
        getSeries("requests", capability.value(), window.range()).points(),
        getSeries("calls_by_model", capability.value(), window.range()).points());
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
