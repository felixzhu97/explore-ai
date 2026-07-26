package com.ai.metrics.application.model;

import java.util.List;

public record SeriesSnapshot(
        String name,
        String domain,
        String range,
        List<SeriesPoint> points
) {
}
