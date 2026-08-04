package com.ai.metrics.web;

import java.util.List;

public record SeriesResponse(
        String name,
        String domain,
        String range,
        List<SeriesPointResponse> points
) {
}
