package com.ai.metrics.web.dto;

import java.util.List;

public record SeriesResponse(
        String name,
        String domain,
        String range,
        List<SeriesPointResponse> points
) {
}
