package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repository that stores, purges, and pages through AI invocation events. */
public interface AiInvocationEventRepository {
  PageResult findDrilldown(DrilldownQuery query);

  void save(AiInvocationEvent event);

  default int deleteBySessionIds(Collection<String> sessionIds) {
    return 0;
  }

  default int deleteOlderThan(Instant cutoff) {
    return 0;
  }

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
      int size) {}

  record PageResult(List<AiInvocationEvent> items, long total) {}
}
