package com.ai.metrics.controller.dto;

import java.util.List;
import java.util.Map;

/** Documentation. */
public record MetricsOverviewResponse(
    String range,
    long requestCount,
    long errorCount,
    double successRate,
    double errorRate,
    Double latencyP50Ms,
    Double latencyP95Ms,
    Long promptTokens,
    Long completionTokens,
    List<NamedCountResponse> requestsByDomain,
    Map<String, Object> domains) {}
