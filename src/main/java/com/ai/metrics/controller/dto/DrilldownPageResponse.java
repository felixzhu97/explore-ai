package com.ai.metrics.controller.dto;

import java.util.List;

/** Documentation. */
public record DrilldownPageResponse(
    List<InvocationEventResponse> items, long total, int page, int size) {}
