package com.ai.metrics.controller.dto;

import java.util.List;

public record SeriesResponse(
    String name, MetricsDomain domain, MetricsRange range, List<SeriesPointResponse> points) {}
