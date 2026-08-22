package com.ai.metrics.service.model;

import com.ai.metrics.domain.model.AiInvocationEvent;
import java.util.List;

/** Documentation. */
public record DrilldownPage(List<AiInvocationEvent> items, long total, int page, int size) {}
