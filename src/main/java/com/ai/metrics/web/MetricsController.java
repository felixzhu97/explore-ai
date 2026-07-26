package com.ai.metrics.web;

import com.ai.metrics.application.model.DrilldownPage;
import com.ai.metrics.application.model.MetricsDomainSnapshot;
import com.ai.metrics.application.model.MetricsOverview;
import com.ai.metrics.application.model.SeriesSnapshot;
import com.ai.metrics.application.usecase.MetricsUseCase;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.web.dto.DrilldownPageResponse;
import com.ai.metrics.web.dto.InvocationEventResponse;
import com.ai.metrics.web.dto.MetricsDomainResponse;
import com.ai.metrics.web.dto.MetricsOverviewResponse;
import com.ai.metrics.web.dto.NamedCountResponse;
import com.ai.metrics.web.dto.SeriesPointResponse;
import com.ai.metrics.web.dto.SeriesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "AI metrics overview and drill-down")
public class MetricsController {

    private final MetricsUseCase metricsUseCase;

    public MetricsController(MetricsUseCase metricsUseCase) {
        this.metricsUseCase = metricsUseCase;
    }

    @GetMapping("/overview")
    @Operation(summary = "AI metrics overview")
    public ResponseEntity<MetricsOverviewResponse> overview(
            @RequestParam(defaultValue = "7d") String range) {
        return ResponseEntity.ok(toOverview(metricsUseCase.overview(range)));
    }

    @GetMapping("/domains/{domain}")
    @Operation(summary = "Domain-scoped AI metrics")
    public ResponseEntity<MetricsDomainResponse> domain(
            @PathVariable String domain,
            @RequestParam(defaultValue = "7d") String range) {
        return ResponseEntity.ok(toDomain(metricsUseCase.domain(domain, range)));
    }

    @GetMapping("/series")
    @Operation(summary = "Metrics time series or categorical series")
    public ResponseEntity<SeriesResponse> series(
            @RequestParam String name,
            @RequestParam(required = false) String domain,
            @RequestParam(defaultValue = "7d") String range) {
        return ResponseEntity.ok(toSeries(metricsUseCase.series(name, domain, range)));
    }

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
        return ResponseEntity.ok(toDrilldown(metricsUseCase.drilldown(
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
                page.items().stream().map(this::toEvent).toList(),
                page.total(),
                page.page(),
                page.size());
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
