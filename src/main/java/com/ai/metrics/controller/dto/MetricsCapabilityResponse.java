package com.ai.metrics.controller.dto;

import java.util.List;

public record MetricsCapabilityResponse(
    MetricsCapability capability,
    MetricsRange range,
    long requestCount,
    long errorCount,
    double errorRate,
    Double latencyP50Ms,
    Double latencyP95Ms,
    Long promptTokens,
    Long completionTokens,
    CapabilityInventoryResponse inventory,
    List<SeriesPointResponse> requestSeries,
    List<SeriesPointResponse> modelSeries) {}
