package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.model.AiDomain;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.InvocationOutcome;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repository that stores, purges, and pages through AI invocation events. */
public interface AiInvocationEventRepository {
  /** Finds one page of events that match the query. */
  PageResult findDrilldown(DrilldownQuery query);

  /** Saves the event. */
  void save(AiInvocationEvent event);

  /** Deletes the events of the sessions and returns the count. */
  default int deleteBySessionIds(Collection<String> sessionIds) {
    return 0;
  }

  /** Deletes events older than the cutoff and returns the count. */
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
