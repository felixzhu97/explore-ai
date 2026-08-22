package com.ai.metrics.controller;

import com.ai.metrics.controller.dto.DrilldownPageResponse;
import com.ai.metrics.controller.dto.InvocationEventResponse;
import com.ai.metrics.controller.dto.MetricsDomainResponse;
import com.ai.metrics.controller.dto.MetricsOverviewResponse;
import com.ai.metrics.controller.dto.NamedCountResponse;
import com.ai.metrics.controller.dto.SeriesPointResponse;
import com.ai.metrics.controller.dto.SeriesResponse;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsDomainSnapshot;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.SeriesSnapshot;
import com.ai.metrics.service.usecase.MetricsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Documentation. */
@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "AI metrics overview and drill-down")
public class MetricsController {

  private final MetricsUseCase metricsUseCase;

  /** Documentation. */
  public MetricsController(MetricsUseCase metricsUseCase) {
    this.metricsUseCase = metricsUseCase;
  }

  /** Documentation. */
  @GetMapping("/overview")
  @Operation(summary = "AI metrics overview")
  public ResponseEntity<MetricsOverviewResponse> overview(
      @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toOverview(metricsUseCase.overview(range)));
  }

  /** Documentation. */
  @GetMapping("/domains/{domain}")
  @Operation(summary = "Domain-scoped AI metrics")
  public ResponseEntity<MetricsDomainResponse> domain(
      @PathVariable String domain, @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toDomain(metricsUseCase.domain(domain, range)));
  }

  /** Documentation. */
  @GetMapping("/series")
  @Operation(summary = "Metrics time series or categorical series")
  public ResponseEntity<SeriesResponse> series(
      @RequestParam String name,
      @RequestParam(required = false) String domain,
      @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toSeries(metricsUseCase.series(name, domain, range)));
  }

  /** Documentation. */
  @GetMapping("/drilldown")
  @Operation(summary = "Paged AI invocation events for drill-down")
  public ResponseEntity<DrilldownPageResponse> drilldown(
      @RequestParam(required = false) String domain,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String day,
      @RequestParam(required = false) String outcome,
      @RequestParam(required = false) String model,
      @RequestParam(required = false) String agentType,
      @RequestParam(required = false) String toolName,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(
        toDrilldown(
            metricsUseCase.drilldown(
                domain, from, to, day, outcome, model, agentType, toolName, page, size, range)));
  }

  private MetricsOverviewResponse toOverview(MetricsOverview overview) {
    return new MetricsOverviewResponse(
        overview.range(),
        overview.requestCount(),
        overview.errorCount(),
        overview.successRate(),
        overview.errorRate(),
        overview.latencyP50Ms(),
        overview.latencyP95Ms(),
        overview.promptTokens(),
        overview.completionTokens(),
        overview.requestsByDomain().stream()
            .map(nc -> new NamedCountResponse(nc.name(), nc.count()))
            .toList(),
        overview.domains());
  }

  private MetricsDomainResponse toDomain(MetricsDomainSnapshot snapshot) {
    return new MetricsDomainResponse(
        snapshot.domain(),
        snapshot.range(),
        snapshot.requestCount(),
        snapshot.errorCount(),
        snapshot.errorRate(),
        snapshot.latencyP50Ms(),
        snapshot.latencyP95Ms(),
        snapshot.promptTokens(),
        snapshot.completionTokens(),
        snapshot.inventory(),
        snapshot.requestSeries().stream()
            .map(p -> new SeriesPointResponse(p.label(), p.value()))
            .toList(),
        snapshot.modelSeries().stream()
            .map(p -> new SeriesPointResponse(p.label(), p.value()))
            .toList());
  }

  private SeriesResponse toSeries(SeriesSnapshot snapshot) {
    return new SeriesResponse(
        snapshot.name(),
        snapshot.domain(),
        snapshot.range(),
        snapshot.points().stream()
            .map(p -> new SeriesPointResponse(p.label(), p.value()))
            .toList());
  }

  private DrilldownPageResponse toDrilldown(DrilldownPage page) {
    return new DrilldownPageResponse(
        page.items().stream().map(this::toEvent).toList(), page.total(), page.page(), page.size());
  }

  private InvocationEventResponse toEvent(AiInvocationEvent event) {
    return new InvocationEventResponse(
        event.getId().toString(),
        event.getOccurredAt(),
        event.getDomain().value(),
        event.getOperation(),
        event.getOutcome().value(),
        event.getLatencyMs(),
        event.getProvider(),
        event.getModel(),
        event.getSessionId(),
        event.getDocumentId(),
        event.getAgentType(),
        event.getToolName(),
        event.getPromptTokens(),
        event.getCompletionTokens(),
        event.getErrorCode(),
        event.getErrorMessage());
  }
}
