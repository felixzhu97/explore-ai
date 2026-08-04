package com.ai.metrics.application;

import com.ai.metrics.domain.AiInvocationEvent;

import java.util.List;

public record DrilldownPage(
        List<AiInvocationEvent> items,
        long total,
        int page,
        int size
) {
}
