package com.ai.metrics.controller.dto;

import java.util.List;

public record MetricsOverviewResponse(
    MetricsRange range,
    long requestCount,
    long errorCount,
    double successRate,
    double errorRate,
    Double latencyP50Ms,
    Double latencyP95Ms,
    Long promptTokens,
    Long completionTokens,
    List<NamedCountResponse> requestsByDomain,
    MetricsDomainsResponse domains) {}
