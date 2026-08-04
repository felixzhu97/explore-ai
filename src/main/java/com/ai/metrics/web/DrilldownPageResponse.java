package com.ai.metrics.web;

import java.util.List;

public record DrilldownPageResponse(
        List<InvocationEventResponse> items,
        long total,
        int page,
        int size
) {
}
