package com.ai.metrics.service.model;

import java.util.List;

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
    DomainInventory inventory,
    List<SeriesPoint> requestSeries,
    List<SeriesPoint> modelSeries) {}
