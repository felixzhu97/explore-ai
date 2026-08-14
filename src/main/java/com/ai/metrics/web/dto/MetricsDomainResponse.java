package com.ai.metrics.web.dto;

import java.util.List;
import java.util.Map;

/** Documentation. */
public record MetricsDomainResponse(
    String domain,
    String range,
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
