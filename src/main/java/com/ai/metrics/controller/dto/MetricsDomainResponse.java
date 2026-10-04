package com.ai.metrics.controller.dto;

import java.util.List;
import java.util.Map;

public record MetricsDomainResponse(
    MetricsDomain domain,
    MetricsRange range,
    long requestCount,
    long errorCount,
    double errorRate,
    Double latencyP50Ms,
    Double latencyP95Ms,
    Long promptTokens,
    Long completionTokens,
    Map<String, Object> inventory,
    List<SeriesPointResponse> requestSeries,
    List<SeriesPointResponse> modelSeries) {}
