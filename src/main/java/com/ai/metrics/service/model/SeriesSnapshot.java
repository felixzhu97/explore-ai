package com.ai.metrics.service.model;

import java.util.List;

public record SeriesSnapshot(
    String name, String capability, String range, List<SeriesPoint> points) {}
