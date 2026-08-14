package com.ai.metrics.application.model;

import com.ai.metrics.domain.model.AiInvocationEvent;
import java.util.List;

/** Documentation. */
public record DrilldownPage(List<AiInvocationEvent> items, long total, int page, int size) {}
