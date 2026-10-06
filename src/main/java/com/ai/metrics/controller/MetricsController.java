package com.ai.metrics.controller;

import com.ai.metrics.controller.dto.AgentsInventoryResponse;
import com.ai.metrics.controller.dto.ChatInventoryResponse;
import com.ai.metrics.controller.dto.DomainInventoryResponse;
import com.ai.metrics.controller.dto.DrilldownPageResponse;
import com.ai.metrics.controller.dto.InvocationEventResponse;
import com.ai.metrics.controller.dto.McpInventoryResponse;
import com.ai.metrics.controller.dto.MetricsDomain;
import com.ai.metrics.controller.dto.MetricsDomainResponse;
import com.ai.metrics.controller.dto.MetricsDomainsResponse;
import com.ai.metrics.controller.dto.MetricsOutcome;
import com.ai.metrics.controller.dto.MetricsOverviewResponse;
import com.ai.metrics.controller.dto.MetricsRange;
import com.ai.metrics.controller.dto.NamedCountResponse;
import com.ai.metrics.controller.dto.RagInventoryResponse;
import com.ai.metrics.controller.dto.RequestsInventoryResponse;
import com.ai.metrics.controller.dto.SeriesPointResponse;
import com.ai.metrics.controller.dto.SeriesResponse;
import com.ai.metrics.controller.dto.SystemInventoryResponse;
import com.ai.metrics.controller.dto.ToolsInventoryResponse;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.service.MetricsService;
import com.ai.metrics.service.model.DomainInventory;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsDomainSnapshot;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.OverviewDomains;
import com.ai.metrics.service.model.SeriesSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "AI metrics overview and drill-down")
@RequiredArgsConstructor
public class MetricsController {

  private final MetricsService metricsService;

  @GetMapping("/overview")
  @Operation(summary = "AI metrics overview")
  public ResponseEntity<MetricsOverviewResponse> getOverview(
      @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toOverview(metricsService.getOverview(range)));
  }

  @GetMapping("/series")
  @Operation(summary = "Metrics time series or categorical series")
  public ResponseEntity<SeriesResponse> getSeries(
      @RequestParam String name,
      @RequestParam(required = false) String domain,
      @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toSeries(metricsService.getSeries(name, domain, range)));
  }

  @GetMapping("/drilldown")
  @Operation(summary = "Paged AI invocation events for drill-down")
  public ResponseEntity<DrilldownPageResponse> getDrilldown(
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
            metricsService.getDrilldown(
                domain, from, to, day, outcome, model, agentType, toolName, page, size, range)));
  }

  @GetMapping("/domains/{domain}")
  @Operation(summary = "Domain-scoped AI metrics")
  public ResponseEntity<MetricsDomainResponse> getDomain(
      @PathVariable String domain, @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toDomain(metricsService.getDomain(domain, range)));
  }

  private MetricsOverviewResponse toOverview(MetricsOverview overview) {
    return new MetricsOverviewResponse(
        MetricsRange.fromValue(overview.range()),
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
        toDomains(overview.domains()));
  }

  private MetricsDomainsResponse toDomains(OverviewDomains domains) {
    return new MetricsDomainsResponse(
        ChatInventoryResponse.from(domains.chat()),
        RagInventoryResponse.from(domains.rag()),
        AgentsInventoryResponse.from(domains.agents()),
        McpInventoryResponse.from(domains.mcp()),
        new SystemInventoryResponse(domains.system()));
  }

  private DomainInventoryResponse toInventory(DomainInventory inventory) {
    return switch (inventory) {
      case DomainInventory.Chat chat -> ChatInventoryResponse.from(chat.inventory());
      case DomainInventory.Rag rag -> RagInventoryResponse.from(rag.inventory());
      case DomainInventory.Agents agents -> AgentsInventoryResponse.from(agents.health());
      case DomainInventory.Tools tools ->
          new ToolsInventoryResponse(
              tools.topTools().stream()
                  .map(nc -> new NamedCountResponse(nc.name(), nc.count()))
                  .toList());
      case DomainInventory.Requests totals ->
          new RequestsInventoryResponse(totals.requests(), totals.errors());
    };
  }

  private MetricsDomainResponse toDomain(MetricsDomainSnapshot snapshot) {
    return new MetricsDomainResponse(
        MetricsDomain.fromValue(snapshot.domain()),
        MetricsRange.fromValue(snapshot.range()),
        snapshot.requestCount(),
        snapshot.errorCount(),
        snapshot.errorRate(),
        snapshot.latencyP50Ms(),
        snapshot.latencyP95Ms(),
        snapshot.promptTokens(),
        snapshot.completionTokens(),
        toInventory(snapshot.inventory()),
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
        snapshot.domain() == null ? null : MetricsDomain.fromValue(snapshot.domain()),
        MetricsRange.fromValue(snapshot.range()),
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
        MetricsDomain.from(event.getDomain()),
        event.getOperation(),
        MetricsOutcome.from(event.getOutcome()),
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
