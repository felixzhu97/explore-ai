package com.ai.metrics.test.fixture;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/** In-memory {@link AiInvocationEventRepository} for service-layer unit tests. */
public class FakeAiInvocationEventRepository implements AiInvocationEventRepository {

  public final List<AiInvocationEvent> events = new ArrayList<>();
  public DrilldownQuery lastQuery;

  @Override
  public void save(AiInvocationEvent event) {
    events.add(event);
  }

  @Override
  public PageResult findDrilldown(DrilldownQuery query) {
    lastQuery = query;
    return new PageResult(events, events.size());
  }

  @Override
  public int deleteBySessionIds(Collection<String> sessionIds) {
    return removeAll(event -> sessionIds.contains(event.getSessionId()));
  }

  @Override
  public int deleteOlderThan(Instant cutoff) {
    return removeAll(event -> event.getOccurredAt().isBefore(cutoff));
  }

  @Override
  public int deleteByOwnerKey(OwnerKey ownerKey) {
    return removeAll(event -> event.getOwnerKey().equals(ownerKey));
  }

  @Override
  public int reassignOwnerKey(OwnerKey from, OwnerKey to) {
    throw new UnsupportedOperationException("events are immutable in memory");
  }

  private int removeAll(Predicate<AiInvocationEvent> matches) {
    int before = events.size();
    events.removeIf(matches);
    return before - events.size();
  }
}
