package com.ai.metrics.controller.dto;

import java.util.List;

public record SeriesResponse(
    String name,
    MetricsCapability capability,
    MetricsRange range,
    List<SeriesPointResponse> points) {}
