package com.ai.metrics.service.model;

import java.util.List;
import java.util.Map;

/** Documentation. */
public record MetricsDomainSnapshot(
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
    List<SeriesPoint> requestSeries,
    List<SeriesPoint> modelSeries) {}
