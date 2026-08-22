package com.ai.metrics.controller.dto;

import java.util.List;

/** Documentation. */
public record SeriesResponse(
    String name, String domain, String range, List<SeriesPointResponse> points) {}
