package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Documentation. */
public interface AiInvocationEventRepository {
  /** Documentation. */
  void save(AiInvocationEvent event);

  /** Documentation. */
  default int deleteBySessionIds(Collection<String> sessionIds) {
    return 0;
  }

  /** Documentation. */
  default int deleteOlderThan(Instant cutoff) {
    return 0;
  }

  /** Documentation. */
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

  /** Documentation. */
  record PageResult(List<AiInvocationEvent> items, long total) {}

  /** Documentation. */
  PageResult findDrilldown(DrilldownQuery query);
}
