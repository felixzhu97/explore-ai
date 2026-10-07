package com.ai.metrics.controller;

import com.ai.metrics.controller.dto.AgentsInventoryResponse;
import com.ai.metrics.controller.dto.CapabilityInventoryResponse;
import com.ai.metrics.controller.dto.ChatInventoryResponse;
import com.ai.metrics.controller.dto.DrilldownPageResponse;
import com.ai.metrics.controller.dto.InvocationEventResponse;
import com.ai.metrics.controller.dto.McpInventoryResponse;
import com.ai.metrics.controller.dto.MetricsCapabilitiesResponse;
import com.ai.metrics.controller.dto.MetricsCapability;
import com.ai.metrics.controller.dto.MetricsCapabilityResponse;
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
import com.ai.metrics.service.model.CapabilityInventory;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsCapabilitySnapshot;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.OverviewCapabilities;
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
      @RequestParam(required = false) String capability,
      @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toSeries(metricsService.getSeries(name, capability, range)));
  }

  @GetMapping("/drilldown")
  @Operation(summary = "Paged AI invocation events for drill-down")
  public ResponseEntity<DrilldownPageResponse> getDrilldown(
      @RequestParam(required = false) String capability,
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
                capability,
                from,
                to,
                day,
                outcome,
                model,
                agentType,
                toolName,
                page,
                size,
                range)));
  }

  @GetMapping("/capabilities/{capability}")
  @Operation(summary = "Capability-scoped AI metrics")
  public ResponseEntity<MetricsCapabilityResponse> getCapability(
      @PathVariable String capability, @RequestParam(defaultValue = "7d") String range) {
    return ResponseEntity.ok(toCapability(metricsService.getCapability(capability, range)));
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
        overview.requestsByCapability().stream()
            .map(nc -> new NamedCountResponse(nc.name(), nc.count()))
            .toList(),
        toCapabilities(overview.capabilities()));
  }

  private MetricsCapabilitiesResponse toCapabilities(OverviewCapabilities capabilities) {
    return new MetricsCapabilitiesResponse(
        ChatInventoryResponse.createResponse(capabilities.chat()),
        RagInventoryResponse.createResponse(capabilities.rag()),
        AgentsInventoryResponse.createResponse(capabilities.agents()),
        McpInventoryResponse.createResponse(capabilities.mcp()),
        new SystemInventoryResponse(capabilities.system()));
  }

  private CapabilityInventoryResponse toInventory(CapabilityInventory inventory) {
    return switch (inventory) {
      case CapabilityInventory.Chat chat -> ChatInventoryResponse.createResponse(chat.inventory());
      case CapabilityInventory.Rag rag -> RagInventoryResponse.createResponse(rag.inventory());
      case CapabilityInventory.Agents agents ->
          AgentsInventoryResponse.createResponse(agents.health());
      case CapabilityInventory.Tools tools ->
          new ToolsInventoryResponse(
              tools.topTools().stream()
                  .map(nc -> new NamedCountResponse(nc.name(), nc.count()))
                  .toList());
      case CapabilityInventory.Requests totals ->
          new RequestsInventoryResponse(totals.requests(), totals.errors());
    };
  }

  private MetricsCapabilityResponse toCapability(MetricsCapabilitySnapshot snapshot) {
    return new MetricsCapabilityResponse(
        MetricsCapability.fromValue(snapshot.capability()),
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
        snapshot.capability() == null ? null : MetricsCapability.fromValue(snapshot.capability()),
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
        MetricsCapability.createResponse(event.getCapability()),
        event.getOperation(),
        MetricsOutcome.createResponse(event.getOutcome()),
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
