package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AiInvocationEventRepository {

    void save(AiInvocationEvent event);

    record DrilldownQuery(
            Optional<AiDomain> domain,
            Optional<Instant> from,
            Optional<Instant> to,
            Optional<String> day,
            Optional<InvocationOutcome> outcome,
            Optional<String> model,
            Optional<String> agentType,
            Optional<String> toolName,
            int page,
            int size
    ) {
    }

    record PageResult(List<AiInvocationEvent> items, long total) {
    }

    PageResult findDrilldown(DrilldownQuery query);
}
