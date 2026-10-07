package com.ai.metrics.service.model;

import java.util.List;

public record MetricsCapabilitySnapshot(
    String capability,
    String range,
    long requestCount,
    long errorCount,
    double errorRate,
    Double latencyP50Ms,
    Double latencyP95Ms,
    Long promptTokens,
    Long completionTokens,
    CapabilityInventory inventory,
    List<SeriesPoint> requestSeries,
    List<SeriesPoint> modelSeries) {}
