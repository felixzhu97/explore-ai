package com.ai.metrics.application.model;

import java.util.List;

/** Documentation. */
public record SeriesSnapshot(String name, String domain, String range, List<SeriesPoint> points) {}
