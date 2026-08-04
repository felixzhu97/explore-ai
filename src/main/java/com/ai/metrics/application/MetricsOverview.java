package com.ai.metrics.application;

import java.util.List;
import java.util.Map;

public record MetricsOverview(
        String range,
        long requestCount,
        long errorCount,
        double successRate,
        double errorRate,
        Double latencyP50Ms,
        Double latencyP95Ms,
        Long promptTokens,
        Long completionTokens,
        List<NamedCount> requestsByDomain,
        Map<String, Object> domains
) {
}
